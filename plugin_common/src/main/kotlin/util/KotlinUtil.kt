package util

import com.android.ide.common.process.BaseProcessOutputHandler
import com.android.ide.common.process.CachedProcessOutputHandler
import com.android.ide.common.process.DefaultProcessExecutor
import com.android.ide.common.process.ProcessInfoBuilder
import com.android.utils.LineCollector
import com.android.utils.StdLogger
import com.google.gson.Gson
import java.io.File
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

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
fun getClassDirAndName(name: String, split: String): Pair<String, String> {
    if (name.contains(split)) {
        val index = name.lastIndexOf(split)
        val className = name.substring(index + 1)
        val directory = name.substring(0, index)
        return directory to className
    } else {
        return "" to name
    }

}

fun invokeProperty(
    instance: Any, // 实例
    propertyName: String,
    newValue: Any
) {
    val property = instance::class.declaredMemberProperties
        .firstOrNull { it.name == propertyName }
    property?.apply {
        property.isAccessible = true
        property.getter.call(instance, newValue)
    }
}

fun invokeOverloadedMethod(
    instance: Any, // 实例
    methodName: String, // 方法名称
    parameterTypes: List<String>? = null, // 参数类型列表
    vararg args: Any? // 方法实际参数
): Any? {
    try {
        // 获取同名方法
        val method = instance::class.declaredFunctions.firstOrNull { function ->
            if (parameterTypes == null) {
                function.name == methodName && function.parameters.size - 1 == args.size
            } else {
                function.name == methodName && function.parameters.size - 1 == args.size
                        && function.parameters.drop(1)
                    .map { it.type.classifier.toString() } == parameterTypes
            }
        }
        // 如果未找到方法，抛出异常
        requireNotNull(method) { "Method $methodName with specified parameter types not found" }
        // 设置为可访问
        method.isAccessible = true
        // 调用方法
        return method.call(instance, *args)
    } catch (e: Exception) {
        e.printStackTrace()
        throw IllegalStateException("Reflection call to '$methodName' failed", e)
    }
}


/**
 * 读取混淆后规则为map
 */
fun fileToClassMappingMap(
    file: File,
    isReplace: Boolean = true
): LinkedHashMap<String, String> {
    val hashMap = LinkedHashMap<String, String>()
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


/**
 * 满足正则规则
 */
 fun inRegex(patterns: List<Regex>, className: String): Boolean {
    for (regex in patterns) {
        if (regex.matches(className)) {
            return true
        }
    }
    return false
}

fun HashSet<String>.toPatterns(): List<Regex> {
  return  this.map { pattern ->
        pattern
            .replace("*", ".*") // 将 '*' 替换为 '.*'（匹配零个或多个字符）
            .replace("?", ".?") // 将 '?' 替换为 '.?'（匹配零个或一个字符）
            .replace("+", ".+") // 将 '+' 替换为 '.+'（匹配一个或多个字符）
            .toRegex() // 将其转换为正则表达式
    }
}

