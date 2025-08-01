package com.kotlin

import com.android.SdkConstants
import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.artifact.impl.ArtifactsImpl
import com.android.build.api.artifact.impl.InAndOutDirectoryOperationRequestImpl
import com.android.build.api.component.analytics.AnalyticsEnabledArtifacts
import com.android.build.api.component.analytics.AnalyticsEnabledInAndOutDirectoryOperationRequest
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.res.Aapt2FromMaven.Companion.create
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.services.Aapt2DaemonBuildService
import com.android.build.gradle.internal.services.Aapt2Input
import com.android.build.gradle.internal.services.Aapt2ThreadPoolBuildService
import com.android.build.gradle.internal.services.getBuildService
import com.android.build.gradle.internal.services.getBuildServiceName
import com.android.build.gradle.internal.tasks.MergeFileTask
import com.android.build.gradle.internal.utils.setDisallowChanges
import com.kotlin.model.ActivityGuardExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.register
import util.fileToClassMappingMap
import util.invokeOverloadedMethod

/**
 * Created by DengLongFei
 * 2024/12/27
 */
class ActivityGuardPlugin86 {
    fun apply(project: Project) {
        val actGuard = project.extensions.create("actGuard", ActivityGuardExtension::class.java)
            ?: ActivityGuardExtension()
        project.plugins.withType(AppPlugin::class.java) {

            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            androidComponents.onVariants { variant ->


                if (!actGuard.isEnable) {
                    println("activityGuard:Not executed, please open actGuard.enable ")
                    return@onVariants
                }
                if (!variant.isMinifyEnabled) {
                    println("activityGuard:Not executed, please open isMinifyEnabled ")
                    return@onVariants
                }
                val artifacts = if (variant.artifacts is AnalyticsEnabledArtifacts) {
                    (variant.artifacts as AnalyticsEnabledArtifacts).delegate as ArtifactsImpl
                } else {
                    variant.artifacts as ArtifactsImpl
                }
                println("activityGuard: start executed...")
                val variantName = variant.name.capitalized()
                //bundle 资源
                val bundleTask =
                    project.tasks.register<ActivityGuardBundleTask>("activityGuard${variantName}BundleTask")
                bundleTask.configure {
                    it.bundleResFiles.set(artifacts.get(InternalArtifactType.LINKED_RESOURCES_FOR_BUNDLE_PROTO_FORMAT))
                    it.aaptProguardFile.set(artifacts.get(InternalArtifactType.AAPT_PROGUARD_FILE))
                    project.layout.buildDirectory.file(
                        "intermediates/activityGuardBundleResTask/" +
                                "${variantName}/mapping.txt"
                    ).also { file ->
                        it.outputFile.set(file)
                    }

                }
                //apk 资源
                val apkTask =
                    project.tasks.register<ActivityGuardApkTask>("activityGuard${variantName}ApkTask")

                var request = variant.artifacts.use(apkTask)
                    .wiredWithDirectories(
                        ActivityGuardApkTask::inputProcessedRes,
                        ActivityGuardApkTask::outputProcessedRes
                    )
                request = if (request is AnalyticsEnabledInAndOutDirectoryOperationRequest) {
                    request.delegate as InAndOutDirectoryOperationRequestImpl
                } else {
                    request as InAndOutDirectoryOperationRequestImpl
                }
                val transformationRequest = invokeOverloadedMethod(
                    request,
                    "toTransformMany",
                    parameterTypes = listOf("ArtifactTypeT"),
                    InternalArtifactType.LINKED_RESOURCES_BINARY_FORMAT
                ) as ArtifactTransformationRequest<ActivityGuardApkTask>

                apkTask.configure {
                    buildAapt2Input(project, it.aapt2)
                    project.layout.buildDirectory.dir(
                        "intermediates/activityGuardApkResTask/" +
                                "${variantName}/${apkTask.name}"
                    ).also { dir ->
                        it.temDirectory.set(dir)
                    }
                    it.transformationRequest.set(transformationRequest)
                    it.classMapping.set(
                        bundleTask.flatMap { task ->
                            task.outputFile.map { out ->
                                val file = out.asFile
                                fileToClassMappingMap(file, false)
                            }
                        })
                }

                //class
                val classTask =
                    project.tasks.register<ActivityGuardClassTask>("activityGuard${variantName}ClassTask")
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(classTask)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        ActivityGuardClassTask::allJars,
                        ActivityGuardClassTask::allDirectories,
                        ActivityGuardClassTask::output
                    )
                classTask.configure {
                    it.classMapping.set(bundleTask.flatMap { task ->
                        task.outputFile.map { out ->
                            fileToClassMappingMap(out.asFile, true)
                        }
                    })
                }

            }
        }
    }

    fun <ServiceT : BuildService<ParamsT>, ParamsT : BuildServiceParameters> getBuildService(
        buildServiceRegistry: BuildServiceRegistry,
        buildServiceClass: Class<ServiceT>
    ): Provider<ServiceT> {
        val serviceName = getBuildServiceName(buildServiceClass)
        return buildServiceRegistry.registerIfAbsent(serviceName, buildServiceClass) {
            throw IllegalStateException("Service $serviceName is not registered.")
        }
    }

    /**
     * 配置aapt
     */
    private fun buildAapt2Input(project: Project, aapt2Input: Aapt2Input) {
        aapt2Input.buildService.setDisallowChanges(
            getBuildService(project.gradle.sharedServices, Aapt2DaemonBuildService::class.java)
        )
        aapt2Input.threadPoolBuildService.setDisallowChanges(
            getBuildService(project.gradle.sharedServices, Aapt2ThreadPoolBuildService::class.java)
        )
        val aapt2Bin =
            create(project) { option -> System.getenv(option.propertyName) }
        aapt2Input.binaryDirectory.setFrom(aapt2Bin.aapt2Directory)
        aapt2Input.version.setDisallowChanges(aapt2Bin.version)
        aapt2Input.maxWorkerCount.setDisallowChanges(
            project.gradle.startParameter.maxWorkerCount
        )
    }
}