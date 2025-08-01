package com.kotlin.handle

import com.kotlin.model.ActivityGuardExtension
import com.kotlin.model.ClassInfo
import org.gradle.api.Project
import util.ObfuscatorUtil
import util.createDirAndFile
import util.mappingFileToMap
import util.saveClassMappingFile
import java.io.File

/**
 * Created by DengLongFei
 * 2024/12/26
 */
class HandleAaptProguardFile(
    private val project: Project,
    private val proguardFile: File,
    private val dirFile: File,
    private val actGuard: ActivityGuardExtension
) {


    private val obfuscatorUtil by lazy {
        ObfuscatorUtil(
            split = ".", classNameCharPool = actGuard.classNameCharPool,
            dirNameCharPool = actGuard.dirNameCharPool,
            outObfuscatedDir = actGuard.outObfuscatedDir,
        )
    }

    private lateinit var mappingFile: File

    private var useClassMapping: Map<String, String> = mapOf()

    fun getClassMapping(): Pair<LinkedHashMap<String, ClassInfo>, LinkedHashMap<String, String>> {
        //保存在app目录下的mapping
        mappingFile = project.layout.projectDirectory.file("mapping.txt").asFile
        if (!mappingFile.exists()) {
            mappingFile.createNewFile()
        }
        return generateClassMapping(proguardFile, mappingFile)
    }

    /**
     * 生成混淆后名称 键值对
     */
    private fun generateClassMapping(
        proguardFile: File,
        mappingFile: File
    ): Pair<LinkedHashMap<String, ClassInfo>, LinkedHashMap<String, String>> {
        //读取保存的
        val (classMapping, dirMapping) = mappingFileToMap(mappingFile)
        obfuscatorUtil.initMap(classMapping, dirMapping)

        val classRegex = Regex("-keep class ([\\w\\.]+) \\{")
        proguardFile.readLines().forEach { line ->
            val matchResult = classRegex.find(line)
            if (matchResult != null) {
                val className = matchResult.groupValues[1]
                val obfuscatorClassName = obfuscatorClassName(className, classMapping)
                if (obfuscatorClassName != className) {
                    classMapping[className] = ClassInfo(obfuscatorClassName, false)
                }
            }
        }
        //过滤白名单
        val mapping = classMapping.filter { !inWhiteList(it.key) } as LinkedHashMap
        return mapping to dirMapping

    }

    /**
     * 混淆后的类名
     */
    private fun obfuscatorClassName(
        className: String,
        classMapping: LinkedHashMap<String, ClassInfo>
    ): String {
        if (className.startsWith("androidx.")
            || className.startsWith("android.")
            || className.startsWith("com.google.")
        ) {
            return className
        }
        //在白名单
        if (inWhiteList(className)) {
            return className
        }
        //已经缓存过混淆名称
        val obfuscatorName = classMapping[className]
        if (obfuscatorName != null) {
            return obfuscatorName.obfuscatorClassName
        }
        return obfuscatorUtil.getObfuscatedClassName(className)
    }


    //白名单正则表达式
    private val regexPatterns by lazy {
        actGuard.whiteClassList.map { pattern ->
            pattern
                .replace("*", ".*") // 将 '*' 替换为 '.*'（匹配零个或多个字符）
                .replace("?", ".?") // 将 '?' 替换为 '.?'（匹配零个或一个字符）
                .replace("+", ".+") // 将 '+' 替换为 '.+'（匹配一个或多个字符）
                .toRegex() // 将其转换为正则表达式
        }
    }


    /**
     * 是否在白名单中
     */
    private fun inWhiteList(className: String): Boolean {
        for (regex in regexPatterns) {
            if (regex.matches(className)) {
                return true
            }
        }
        return (className.startsWith("androidx.")
                || className.startsWith("android.")
                || className.startsWith("com.google."))
    }


    fun replaceProguardFile(
        classMapping: Map<String, ClassInfo>,
        dirMapping: Map<String, String>,
    ) {
        val outFile = File(dirFile, "mapping.txt").also {
            if (it.exists()) {
                it.delete()
            }
            it.createNewFile()
        }
        //使用的映射
        useClassMapping =
            classMapping.filter { it.value.isUse }.mapValues { it.value.obfuscatorClassName }
        //修改proguardFile文件
        replaceProguardFile(proguardFile.absolutePath, useClassMapping)
        //保存混淆后的类名文件
        saveClassMappingFile(outFile.absolutePath, useClassMapping, dirMapping)
        saveClassMappingFile(mappingFile.absolutePath, useClassMapping, dirMapping)
        //保存未使用的类名
        saveClassMappingFile(
            createDirAndFile(
                outFile.parentFile.absolutePath,
                "unused.txt"
            ).also { it.createNewFile() }.absolutePath,
            classMapping.filter { !it.value.isUse }.mapValues { it.value.obfuscatorClassName },
            mapOf()
        )
    }

    /**
     * 修改proguardFile文件
     */
    private fun replaceProguardFile(filePath: String, mapping: Map<String, String>) {
        val file = File(filePath)
        var content = file.readText()
        for ((oldText, newText) in mapping) {
            content = content.replace(" $oldText ", " $newText ")
        }
        file.writeText(content)
        val keepString = "\n#keep插件ActivityGuard混淆后的代码\n" +
                "-keep class ${actGuard.outObfuscatedDir}.**{ *; }"
        if (!content.contains(keepString)) {
            file.writeText(content + keepString)
        }
    }
}