package com.kotlin.model

/**
 * Created by DengLongFei
 * 2024/11/26
 */
data class ClassInfo(
    val obfuscatorClassName: String,//混淆后的类名
    var isUse: Boolean,//是否使用
) {

}