package com.kotlin

import com.android.aapt.Resources
import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.variant.BuiltArtifact
import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.profile.ProfileAwareWorkAction
import com.android.build.gradle.internal.services.Aapt2DaemonServiceKey
import com.android.build.gradle.internal.services.Aapt2Input
import com.android.build.gradle.internal.services.getAaptDaemon
import com.android.build.gradle.internal.services.registerAaptService
import com.android.build.gradle.internal.tasks.BaseTask
import com.android.builder.internal.aapt.AaptConvertConfig
import com.android.builder.packaging.JarFlinger
import com.kotlin.model.ClassInfo
import com.kotlin.util.changeLayoutXmlName
import com.kotlin.util.changeXmlNodeAttribute
import com.kotlin.util.createDirAndFile
import com.kotlin.util.readByte
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.io.Serializable
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlin.io.path.Path

/**
 * Created by DengLongFei
 * 2024/12/09
 */
abstract class ObfuscatorApkTask : BaseTask() {

    @get:Input
    abstract val classMapping: MapProperty<String, String>

    @get:OutputDirectory
    abstract val temDirectory: DirectoryProperty

    @get:Nested
    abstract val aapt2: Aapt2Input

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputProcessedRes: DirectoryProperty

    @get:OutputDirectory
    abstract val outputProcessedRes: DirectoryProperty


    @get:Internal
    abstract val transformationRequest: Property<ArtifactTransformationRequest<ObfuscatorApkTask>>

    @TaskAction
    fun taskAction() {
        println(
            "activityGuard:ObfuscatorApkTask----" + inputProcessedRes.get().asFile.absolutePath +
                    " \nout " + outputProcessedRes.get().asFile.absolutePath
        )
        transformationRequest.get().submit(
            this,
            workerExecutor.noIsolation(),
            WorkItem::class.java
        ) { builtArtifact: BuiltArtifact, outputLocation: Directory, param: WorkItemParameters ->
            val inputFile = File(builtArtifact.outputFile)
            param.classMapping.set(classMapping.get())
            param.aapt2ServiceKey.set(aapt2.registerAaptService())
            param.inputApkFile.set(inputFile)
            param.outputApkFile.set(File(outputLocation.asFile, inputFile.name))
            param.temProtoFile.set(File(temDirectory.get().asFile, inputFile.name))
            param.outputApkFile.get().asFile
        }
    }
}

interface WorkItemParameters : WorkParameters, Serializable {
    val inputApkFile: RegularFileProperty
    val outputApkFile: RegularFileProperty
    val temProtoFile: RegularFileProperty
    val aapt2ServiceKey: Property<Aapt2DaemonServiceKey>
    val classMapping: MapProperty<String, String>
}


abstract class WorkItem @Inject constructor() :
    WorkAction<WorkItemParameters> {
    private val logger = Logging.getLogger(ObfuscatorApkTask::class.java)
    override fun execute() {


        parameters.outputApkFile.get().asFile.delete()
        parameters.inputApkFile.asFile.get().copyTo(
            parameters.outputApkFile.get().asFile
        )
        val classMapping = parameters.classMapping.get()
        val aapt2ServiceKey = parameters.aapt2ServiceKey.get()
        val inputApkFile = parameters.inputApkFile.get()
        val temProtoFile = parameters.temProtoFile.get()
        val outputApkFile = parameters.outputApkFile.get()
        //apk to bundle
        getAaptDaemon(aapt2ServiceKey).use {
            it.convert(
                AaptConvertConfig(
                    inputFile = inputApkFile.asFile,
                    outputFile = temProtoFile.asFile,
                    convertToProtos = true
                ),
                LoggerWrapper(logger)
            )
        }
        //修改bundle
        obfuscatorRes(temProtoFile.asFile, classMapping.mapValues {
            ClassInfo(it.value, true)
        })
        //bundle to apk
        getAaptDaemon(aapt2ServiceKey).use {
            it.convert(
                AaptConvertConfig(
                    inputFile = temProtoFile.asFile,
                    outputFile = outputApkFile.asFile,
                    convertToProtos = false
                ),
                LoggerWrapper(logger)
            )
        }
    }


    /**
     * 修改layout和AndroidManifest
     */
    private fun obfuscatorRes(
        inputFile: File,
        classMapping: Map<String, ClassInfo>
    ) {
        val dirName = inputFile.parentFile.absolutePath + "/bundleRes" + inputFile.name
        FileUtils.deleteDirectory(File(dirName))
        val bundleZip = ZipFile(inputFile)
        bundleZip.entries().asSequence().forEach { zipEntry ->
            val path = zipEntry.name
            when {
                path == "resources.pb" -> {
                    val resourceTableByte = readByte(bundleZip, path)
                    createDirAndFile(dirName, path).outputStream().use { out ->
                        out.write(resourceTableByte)
                    }
                }

                path.startsWith("res/layout") -> {
                    val xmlNode = changeLayoutXmlName(bundleZip, path.toString(), classMapping)
                    createDirAndFile(dirName, path.toString()).outputStream()
                        .use { xmlNode.writeTo(it) }
                }

                path == "AndroidManifest.xml" -> {
                    val xmlNode = Resources.XmlNode.parseFrom(readByte(bundleZip, path))
                    var newXmlNode = xmlNode
                    mapOf(
                        "activity" to "name",
                        "service" to "name",
                        "application" to "name",
                        "provider" to "name",
                    ).forEach {
                        newXmlNode =
                            changeXmlNodeAttribute(newXmlNode, it.key, it.value, classMapping)
                    }
                    createDirAndFile(dirName, path).outputStream()
                        .use { newXmlNode.writeTo(it) }
                }

                else -> {
                    createDirAndFile(dirName, path.toString()).outputStream().use { out ->
                        out.write(readByte(bundleZip, path.toString()))
                    }
                }
            }
        }
        //关闭bundleResFiles文件
        bundleZip.close()
        //保存并修改bundleResFiles
        val outBundleResFilePath = inputFile.absolutePath
        File(outBundleResFilePath).delete()
        JarFlinger(
            Path(outBundleResFilePath),
            null
        ).use { jarCreator ->
            jarCreator.addDirectory(
                Path(dirName),
                null,
                null,
                null
            )
        }
    }
}