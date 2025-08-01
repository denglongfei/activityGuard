package com.kotlin.model

/**
 * Created by DengLongFei
 * 2024/11/25
 */
open class ActivityGuardExtension {
    //是否开启
    var isEnable = true

    //白名单
    var whiteClassList = hashSetOf<String>()


    //额外需要混淆的类
    var otherClassList = hashSetOf<String>()

    //混淆后都会输出在这个目录下
    var outObfuscatedDir: String = "guard"

    //类名字字符串
    var classNameCharPool: String = "abcdefghijklmnopqrstuvwxyz0123456789"

    //包名称字符串
    var dirNameCharPool: String = "abcdefghijklmnopqrstuvwxyz"
}