package com.kotlin.util

import com.kotlin.model.ClassInfo

/**
 * Created by DengLongFei
 * 2024/12/16
 */
class ObfuscatorUtil {
    private val alphabet = "abcdefghijklmnopqrstuvwxyz"
    private val digitAlphabet = "abcdefghijklmnopqrstuvwxyz0123456789"

    //保存混淆后的目录名和当前目录下的混淆类名
    private val generatedClasses = mutableMapOf<String, MutableSet<String>>()

    fun initMap(
        classMapping: LinkedHashMap<String, ClassInfo>,
        dirMapping: LinkedHashMap<String, String>
    ) {
        dirMapping.forEach {
            generatedClasses.getOrPut(it.value) { mutableSetOf() }
        }
        classMapping.forEach {
            val (obfuscatorDir, obfuscatorName) = getClassDirAndName(it.value.obfuscatorClassName)
            generatedClasses.getOrPut(obfuscatorDir) { mutableSetOf() }.add(obfuscatorName)
        }
    }

    /**
     * 获取混淆目录
     */
    fun getObfuscatedClassDir(): String {
        val count = generatedClasses.size
        return generateName(count, minLength = 2, charset = alphabet).also {
            generatedClasses.getOrPut(it) { mutableSetOf() }
        }
    }

    /**
     * 获取混淆名称
     */
    fun getObfuscatedClassName(dir: String): String {
        val classList = generatedClasses.getOrPut(dir) { mutableSetOf() }
        return generateName(classList.size, minLength = 1, charset = digitAlphabet).also {
            generatedClasses.getOrPut(dir) { mutableSetOf() }.add(it)
        }
    }

    private fun generateName(counter: Int, minLength: Int, charset: String): String {
        val sb = StringBuilder()
        var value = counter
        if (value < (minLength - 1) * charset.length) {
            value += minLength * charset.length
        }
        //首为字母
        val firstCharIndex = value % alphabet.length
        sb.append(alphabet[firstCharIndex])
        value /= alphabet.length

        while (value > 0){
            val charIndex = value % charset.length
            sb.append(charset[charIndex])
            value /= charset.length
        }
        return sb.toString()
    }

}



