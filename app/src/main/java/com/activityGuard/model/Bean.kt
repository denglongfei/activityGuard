package com.activityGuard.model

import java.io.Serializable

/**
 * Created by DengLongFei
 * 2024/10/28
 */
 data class Bean(val name: String = ""):Serializable
data class Bean1(val name: String = ""):Serializable
data class Bean2(val name: String = ""):Serializable
data class Bean3(val name: String = ""):Serializable{
    fun aaaaaaaaa(Bean1: Bean1): Bean4 {
        val ss =   Bean1.name
        return  Bean4(ss)
    }
}
data class Bean4(val name: String = ""):Serializable