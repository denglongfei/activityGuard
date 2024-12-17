package com.kotlin.util

import com.android.build.gradle.internal.res.Aapt2FromMaven.Companion.create
import com.android.build.gradle.internal.services.Aapt2Input
import com.android.build.gradle.internal.services.getBuildService
import com.android.build.gradle.internal.utils.setDisallowChanges
import com.android.ide.common.process.BaseProcessOutputHandler
import com.android.ide.common.process.CachedProcessOutputHandler
import com.android.ide.common.process.DefaultProcessExecutor
import com.android.ide.common.process.ProcessInfoBuilder
import com.android.utils.LineCollector
import com.android.utils.StdLogger
import com.google.gson.Gson
import org.gradle.api.Project
import java.io.File
import java.io.IOException
import java.io.Writer

/**
 * Created by DengLongFei
 * 2024/10/23
 */

var isDebug = true

val gson by lazy { Gson() }

inline fun <reified T> T.toJson(): String {
    return try {
        this.toString()
    } catch (e: Exception) {
        this.toString()
    }
}

inline fun <reified T> T.toJson2(): String {
    return try {
        gson.toJson(this, T::class.java)
    } catch (e: Exception) {
        this.toString()
    }
}

/**
 * json转对象
 */
inline fun <reified T> String.toObject(): T {
    return gson.fromJson<T>(this, T::class.java)
}

var logFileUtil: LogFileUtil? = null

fun logDebug(log: String) {
    if (isDebug) {
        logFileUtil?.sendLog(log)
    }
}


/**
 * 配置aapt
 */
fun buildAapt2Input(project: Project, aapt2Input: Aapt2Input) {
    aapt2Input.buildService.setDisallowChanges(
        getBuildService(project.gradle.sharedServices)
    )
    aapt2Input.threadPoolBuildService.setDisallowChanges(
        getBuildService(project.gradle.sharedServices)
    )
    val aapt2Bin =
        create(project) { option -> System.getenv(option.propertyName) }
    aapt2Input.binaryDirectory.setFrom(aapt2Bin.aapt2Directory)
    aapt2Input.version.setDisallowChanges(aapt2Bin.version)
    aapt2Input.maxWorkerCount.setDisallowChanges(
        project.gradle.startParameter.maxWorkerCount
    )
}


fun invokeAapt(aapt2: File, vararg args: String): List<String> {
    val processOutputHeader = CachedProcessOutputHandler()
    val processInfoBuilder = ProcessInfoBuilder()
        .setExecutable(aapt2)
        .addArgs(args)
    val processExecutor = DefaultProcessExecutor(StdLogger(StdLogger.Level.ERROR))
    processExecutor
        .execute(processInfoBuilder.createProcess(), processOutputHeader)
        .rethrowFailure()
    val output: BaseProcessOutputHandler.BaseProcessOutput = processOutputHeader.processOutput
    val lineCollector = LineCollector()
    output.processStandardOutputLines(lineCollector)
    return lineCollector.result
}


/**
 * 获取类的 目录和名称
 */
fun getClassDirAndName(name: String, split: String = "."): Pair<String, String> {
    if (name.contains(split)) {
        val index = name.lastIndexOf(split)
        val className = name.substring(index + 1)
        val directory = name.substring(0, index)
        return directory to className
    } else {
        return "" to name
    }

}


