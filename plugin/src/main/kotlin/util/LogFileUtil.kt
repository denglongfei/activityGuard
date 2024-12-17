package com.kotlin.util

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileOutputStream

class LogFileUtil(private val outputStream: FileOutputStream) {
    private val channel = Channel<String>(UNLIMITED)

    init {
        GlobalScope.launch {
            while (true) {
                delay(1000)
                val content = getAllDataFromChannel().joinToString("\n") { it }
                outputStream.write(content.toByteArray())
            }
        }

    }

    fun sendLog(log: String) {
        outputStream.write(log.toByteArray())
        outputStream.write("\n".toByteArray())
    }

    fun closLog(){
        outputStream.close()
    }

    /**
     * 一次性获取 Channel 中的所有数据
     */
    private fun getAllDataFromChannel(): List<String> {
        val dataList = mutableListOf<String>()
        while (true) {
            val data = channel.tryReceive().getOrNull() ?: break // 非阻塞获取数据
            dataList.add(data)
        }
        return dataList
    }
}