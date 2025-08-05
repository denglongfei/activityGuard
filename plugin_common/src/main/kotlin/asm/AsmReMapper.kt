package com.kotlin.asm

import com.kotlin.model.ActivityGuardExtension
import org.objectweb.asm.commons.Remapper
import util.ObfuscatorUtil


/**
 * Created by DengLongFei
 * 2024/12/23
 */
class AsmReMapper(
    private val classMapping: LinkedHashMap<String, String> = LinkedHashMap(),
    private val actGuard: ActivityGuardExtension = ActivityGuardExtension(),
    private val obfuscatorUtil: ObfuscatorUtil
) : Remapper() {
    override fun map(name: String?): String? {
        return obfuscatorDescriptorOrName(name)
    }

    override fun mapValue(value: Any?): Any? {
        return if (value is String) {
            map(value)
        } else {
            super.mapValue(value)
        }
    }

    /**
     * 混淆类名或者描述符
     */
    @Synchronized
    fun obfuscatorDescriptorOrName(name: String?): String? {
        name ?: return null
        return if (name.startsWith("L") || name.startsWith("(")
            || name.startsWith("[") || name.startsWith("<")
        ) {
            name.toObfuscatorDescriptor()
        } else {
            name.toObfuscatorName()
        }
    }

    /**
     * 获取混淆后描述符
     */
    private fun String?.toObfuscatorDescriptor(): String? {
        val descriptor = this
        descriptor ?: return null
        val regex = Regex("L([a-zA-Z0-9_/]+/)([a-zA-Z0-9_$]+)")
        return regex.replace(descriptor) { matchResult ->
            val path = matchResult.groups[1]?.value // 捕获路径部分
            val className = matchResult.groups[2]?.value // 捕获类名部分
            val replacedClass = (path + className).toObfuscatorName()
            "L$replacedClass" // 返回替换后的类描述符
        }
    }

    /**
     * 获取混淆后类名
     */
    private fun String?.toObfuscatorName(): String? {
        val name = this
        name ?: return null
        return if (name.contains("$")) {
            name.split("$")
                .joinToString("$") { getObfuscatorName(it) }
        } else {
            getObfuscatorName(name)
        }

    }

    /**
     * 获取名称
     */
    private fun getObfuscatorName(name: String): String {
        val obName = classMapping[name]
        if (obName != null) {
            return obName
        }
        //在白名单
        if (inRegex(whitePatterns, name)) {
            return name
        }

        //在额外混淆类
        if (inRegex(obfuscatorPatterns, name)) {
            if (name.contains("(") && name.contains(")")){
                return name
            }
            if (name.endsWith("_ViewBinding")){
                val replaceName = name.replace("_ViewBinding", "")
                val mapName = classMapping[replaceName]
                if (mapName != null) {
                    return mapName+"_ViewBinding"
                }
                val newName = obfuscatorUtil.getObfuscatedClassName(replaceName)
                classMapping[replaceName] = newName
                return newName+"_ViewBinding"
            }
            if (name.contains(".")) {
                val replaceName = name.replace(".", "/")
                val mapName = classMapping[replaceName]
                if (mapName != null) {
                    return mapName.replace("/", ".")
                }
                val newName = obfuscatorUtil.getObfuscatedClassName(replaceName)
                classMapping[replaceName] = newName
                return newName.replace("/", ".")
            } else {
                val newName = obfuscatorUtil.getObfuscatedClassName(name)
                classMapping[name] = newName
                return newName
            }

        }
        return name
    }


    /**
     * 满足正则规则
     */
    private fun inRegex(patterns: List<Regex>, className: String): Boolean {
        for (regex in patterns) {
            if (regex.matches(className)) {
                return true
            }
        }
        return false
    }

    //白名单
    private val whitePatterns by lazy {
        actGuard.whiteClassList.map { pattern ->
            pattern
                .replace("*", ".*") // 将 '*' 替换为 '.*'（匹配零个或多个字符）
                .replace("?", ".?") // 将 '?' 替换为 '.?'（匹配零个或一个字符）
                .replace("+", ".+") // 将 '+' 替换为 '.+'（匹配一个或多个字符）
                .toRegex() // 将其转换为正则表达式
        }
    }

    //额外需要混淆的类
    private val obfuscatorPatterns by lazy {
        actGuard.otherClassList.map { pattern ->
            pattern
                .replace("*", ".*") // 将 '*' 替换为 '.*'（匹配零个或多个字符）
                .replace("?", ".?") // 将 '?' 替换为 '.?'（匹配零个或一个字符）
                .replace("+", ".+") // 将 '+' 替换为 '.+'（匹配一个或多个字符）
                .toRegex() // 将其转换为正则表达式
        }
    }
}