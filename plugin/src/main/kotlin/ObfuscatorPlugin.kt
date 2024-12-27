package com.kotlin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.artifact.impl.ArtifactsImpl
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.kotlin.model.ActivityGuardExtension
import com.kotlin.util.buildAapt2Input
import com.kotlin.util.getClassDirAndName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Created by DengLongFei
 * 2024/11/18
 * actGuard
 */
class ObfuscatorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val actGuard = project.extensions.create("actGuard", ActivityGuardExtension::class.java)
        actGuard.enable.convention(true)
        project.plugins.withType(AppPlugin::class.java) {
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            androidComponents.onVariants { variant ->
                if (!actGuard.enable.get()) {
                    return@onVariants
                }
                //val artifacts = variant.artifacts as? ArtifactsImpl ?: return@onVariants
                if (!variant.isMinifyEnabled) {
                    println("activityGuard:Not executed, please open isMinifyEnabled ")
                    return@onVariants
                }

                //资源Bundle混淆
                val taskBundleName = "activityGuard${variant.name.capitalized()}BundleResTask"
                println("activityGuard: start executed...$taskBundleName")
                val taskBundleProvider =
                    project.tasks.register<ObfuscatorBundleResTask>(taskBundleName)
                taskBundleProvider.configure {
                    buildAapt2Input(project, it.aapt2)
                    println("activityGuard: ObfuscatorBundleResTask configure  $taskBundleName")
                    it.bundleResFiles.set(project.layout.buildDirectory.file(
                        "intermediates/linked_res_for_bundle/${variant.name}/bundled-res.ap_"
                    ))
                    it.aaptProguardFile.set(project.layout.buildDirectory.file(
                        "intermediates/aapt_proguard_file/${variant.name}/aapt_rules.txt"
                    ))
                    it.outputFile.set(
                        project.layout.buildDirectory.file(
                            "intermediates/activityGuardBundleResTask/${taskBundleName}/mapping.txt"
                        )
                    )
                }
                taskBundleProvider
                    .dependsOn("bundle${variant.name.capitalized()}Resources")
                    .dependsOn("process${variant.name.capitalized()}Resources")

                //混淆apk资源
                val taskApkName = "activityGuard${variant.name.capitalized()}ApkResTask"
                val taskApkProvider = project.tasks.register<ObfuscatorApkTask>(taskApkName)
                val transformationRequest = variant.artifacts.use(taskApkProvider)
                    .wiredWithDirectories(
                        ObfuscatorApkTask::inputProcessedRes,
                        ObfuscatorApkTask::outputProcessedRes
                    )
                    .toTransformMany(InternalArtifactType.PROCESSED_RES)
                taskApkProvider.configure {
                    buildAapt2Input(project, it.aapt2)
                    it.temDirectory.set(
                        project.layout.buildDirectory.dir(
                            "intermediates/activityGuardApkResTask/${variant.name.capitalized()}/${taskApkName}"
                        )
                    )
                    it.transformationRequest.set(transformationRequest)
                    it.classMapping.set(
                        taskBundleProvider.flatMap { task ->
                            task.outputFile.map { out ->
                                val file = out.asFile
                                fileToClassMappingMap(file, false)
                            }
                        })
                }
                //混淆class
                val transformClassTask =
                    project.tasks.register<ObfuscatorClassTask>("activityGuard${variant.name}ClassTask")
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(transformClassTask)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        ObfuscatorClassTask::allJars,
                        ObfuscatorClassTask::allDirectories,
                        ObfuscatorClassTask::output
                    )
                transformClassTask.configure { params ->
//                    params.logFile.set(
//                        createDirAndFile(
//                            project.layout.buildDirectory.get().asFile.absolutePath,
//                            "intermediates/activityGuardBundleResTask/${taskBundleName}/transformLog.txt"
//                        )
//                    )
                    params.classMapping.set(
                        taskBundleProvider.flatMap {
                            it.outputFile.map { out ->
                                val file = out.asFile
                                fileToClassMappingMap(file)
                            }
                        })
                }

//                //资源优化
//                val optimizeBundleTask =
//                    project.tasks.register<OptimizeBundleTask>("activityGuard${variant.name}optimizeBundleTask")
//                variant.artifacts.use(optimizeBundleTask).wiredWithFiles(
//                    OptimizeBundleTask::inputBundle,
//                    OptimizeBundleTask::outputBundle,
//                    ).toTransform(SingleArtifact.BUNDLE)
//                optimizeBundleTask.configure {
//                    buildAapt2Input(project,it.aapt2)
//                }
            }
        }

    }


    /**
     * 读取混淆后规则为map
     */
    private fun fileToClassMappingMap(
        file: File,
        isReplace: Boolean = true,
    ): HashMap<String, String> {
        val hashMap = hashMapOf<String, String>()
        file.forEachLine { line ->
            if (line.contains("->")) {
                val parts = line.split("->").map { it.trim() }
                if (parts.size == 2) {
                    val (original, obfuscated) = if (isReplace) {
                        val original = parts[0].replace(".", "/")
                        val obfuscated = parts[1].replace(".", "/")
                        original to obfuscated
                    } else {
                        parts[0] to parts[1]
                    }
                    hashMap[original] = obfuscated
                    //兼容butterKnife
                    hashMap[original + "_ViewBinding"] = obfuscated + "_ViewBinding"
                    //兼容hit
                    val split = if (isReplace) "/" else "."
                    val (dir, name) = getClassDirAndName(original, split)
                    val (obfuscatedDir, obfuscatedName) = getClassDirAndName(obfuscated, split)
                    if (dir.isNotEmpty()) {
                        hashMap[dir + split + "Hilt_" + name] =
                            obfuscatedDir + split + "Hilt_" + obfuscatedName
                    } else {
                        hashMap["Hilt_$name"] = "Hilt_$obfuscatedName"
                    }
                }
            }
        }
        return hashMap
    }
}