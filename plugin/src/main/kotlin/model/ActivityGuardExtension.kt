package com.kotlin.model

/**
 * Created by DengLongFei
 * 2024/11/25
 */
open class ActivityGuardExtension {
    var isEnable = true
    var whiteClassList = hashSetOf<String>()

    //类名混淆
    var obfuscatorClassFunction: ((String, String) -> String)? = null

    //目录混淆
    var obfuscatorDirFunction: ((String) -> String)? = null
}