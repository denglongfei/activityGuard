package com.kotlin

import com.android.build.gradle.internal.tasks.BaseTask
import com.kotlin.handle.HandleClassFile
import com.kotlin.model.ActivityGuardExtension
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import util.ObfuscatorUtil
import util.saveClassMappingFile

/**
 * Created by DengLongFei
 * 2024/12/26
 */
abstract class ActivityGuardClassTask : BaseTask() {

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:Input
    abstract val classMapping: MapProperty<String, String>

    private val actGuard: ActivityGuardExtension by lazy {
        project.extensions.getByType(ActivityGuardExtension::class.java)
    }


    @TaskAction
    fun taskAction() {



        //保存在app目录下的mapping
        val mappingFile = project.layout.projectDirectory.file("mapping.txt").asFile
        if (!mappingFile.exists()) {
            mappingFile.createNewFile()
        }
//        //日志文件
//        val logFile = project.layout.projectDirectory.file("logFile.txt").asFile
//        if (!logFile.exists()) {
//            logFile.createNewFile()
//        }
//        logFileUtil?.closLog()
//        logFileUtil = LogFileUtil(logFile.outputStream())

        //混淆工具
        val obfuscatorUtil = ObfuscatorUtil(
            split = "/",
            classNameCharPool = actGuard.classNameCharPool,
            dirNameCharPool = actGuard.dirNameCharPool,
            changePackageList = actGuard.changePackageList
        )
        val classMappingGet = classMapping.get().toMutableMap() as LinkedHashMap

        //class文件处理
        val handleClassFile = HandleClassFile(
            allJars.get(),
            allDirectories.get(),
            output.get().asFile,
            classMappingGet,
            actGuard,
        )
        handleClassFile.chaneClassFile(obfuscatorUtil)


        //更新mapping.txt
        saveClassMappingFile(
            mappingFile.absolutePath,
            classMappingGet.filter { !(it.key.contains("Hilt_") || it.key.contains("_ViewBinding")) }
                .mapKeys { it.key.replace("/", ".") }
                .mapValues { it.value.replace("/", ".") },
            obfuscatorUtil.dirMapping.mapKeys { it.key.replace("/", ".") }
                .mapValues { it.value.replace("/", ".") })
    }


}