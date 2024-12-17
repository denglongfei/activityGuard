package com.kotlin.util

import com.android.tools.r8.internal.C
import com.kotlin.model.ClassInfo
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import java.io.File

/**
 * Created by DengLongFei
 * 2024/11/26
 */


/**
 * 读取混淆后规则为map
 */
fun mappingFileToMap(file: File?): Pair<LinkedHashMap<String, ClassInfo>, LinkedHashMap<String, String>> {
    val classMap = LinkedHashMap<String, ClassInfo>()
    val dirMapping = LinkedHashMap<String, String>()
    if (file == null) {
        return classMap to dirMapping
    }
    file.forEachLine { line ->
        //目录
        if (line.contains("-》")) {
            val parts = line.split("-》").map { it.trim() }
            if (parts.size == 2) {
                val original = parts[0]
                val obfuscated = parts[1]
                dirMapping[original] = obfuscated
            }
        }
        //类名
        if (line.contains("->")) {
            val parts = line.split("->").map { it.trim() }
            if (parts.size == 2) {
                val original = parts[0]
                val obfuscated = parts[1]
                classMap[original] = ClassInfo(obfuscated, false)
            }
        }
    }
    return classMap to dirMapping
}



/**
 * 保存文件
 */
fun saveClassMappingFile(
    filePath: String,
    classMap: Map<String, String>,
    dirMapping: Map<String, String>
) {
    val file = File(filePath)
    file.printWriter().use { writer ->
        if (dirMapping.isNotEmpty()) {
            //保存混淆目录
            writer.println("dir mapping:")
            for ((key, value) in dirMapping) {
                writer.println("$key -》 $value")
            }
            writer.println("\n\n")
        }
        //保存混淆类名
        writer.println("class mapping:")
        for ((key, value) in classMap) {
            writer.println("$key -> $value")
        }
    }
}

/**
 * 创建文件和目录
 */
fun createDirAndFile(dir: String, path: String): File {
    val file = File("$dir/$path")
    file.parentFile.also {
        if (!it.exists()) {
            it.mkdirs()
        }
    }
    return file.also {
        if (it.exists()) {
            it.delete()
        }
        it.createNewFile()
    }
}



