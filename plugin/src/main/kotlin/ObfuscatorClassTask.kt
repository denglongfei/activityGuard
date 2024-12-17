package com.kotlin

import com.kotlin.asm.ClassNameClassVisitor
import com.kotlin.model.ObfuscatorMapping
import com.kotlin.util.LogFileUtil
import com.kotlin.util.logFileUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

abstract class ObfuscatorClassTask : DefaultTask() {
    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @Internal
    val jarPaths = mutableSetOf<String>()

    @get:Input
    abstract val classMapping: MapProperty<String, String>

//    @get:InputFile
//    abstract val logFile: RegularFileProperty

    @TaskAction
    fun taskAction() {
        println("----------" + output.get().asFile.absolutePath)
//        logFileUtil?.closLog()
//        logFileUtil = LogFileUtil(logFile.get().asFile.outputStream())
        val jarOutputStream = JarOutputStream(
            BufferedOutputStream(
                FileOutputStream(
                    output.get().asFile
                )
            )
        )
        // 处理 JAR 文件
        allJars.get().forEach { file ->
            processJarWithASM(file.asFile, jarOutputStream)
        }

        // 处理目录
        allDirectories.get().forEach { dir ->
            processDirectoryWithASM(dir.asFile, jarOutputStream)
        }
        jarOutputStream.close()
//        logFileUtil?.closLog()


    }

    private fun JarOutputStream.writeEntity(name: String, inputStream: InputStream) {
        if (!jarPaths.contains(name)) {
            putNextEntry(JarEntry(name))
            inputStream.copyTo(this)
            closeEntry()
            jarPaths.add(name)
        }
    }

    private fun JarOutputStream.writeEntity(relativePath: String, byteArray: ByteArray) {
        if (!jarPaths.contains(relativePath)) {
            putNextEntry(JarEntry(relativePath))
            write(byteArray)
            closeEntry()
            jarPaths.add(relativePath)
        }
    }

    /**
     * 处理jar
     */
    private fun processJarWithASM(inputJar: File, jarOutput: JarOutputStream) {
        val jarFile = JarFile(inputJar)
        jarFile.entries().iterator().forEach { jarEntry ->
            val entryName = jarEntry.name
            if (entryName.endsWith(".class")) {
                jarFile.getInputStream(jarEntry).use {
                    // 对类文件应用 ASM 处理
                    val classReader = ClassReader(it)
                    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                    val classVisitor = ClassNameClassVisitor(
                        Opcodes.ASM9,
                        classWriter,
                        ObfuscatorMapping(classMapping.get())
                    )
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                    jarOutput.writeEntity(jarEntry.name, classWriter.toByteArray())
                }
            } else {
                // 非类文件直接复制
                jarFile.getInputStream(jarEntry).use { inputStream ->
                    jarOutput.writeEntity(jarEntry.name, inputStream)
                }

            }
        }
        jarFile.close()
    }

    /**
     * 处理目录
     */
    private fun processDirectoryWithASM(inputDir: File, jarOutput: JarOutputStream) {
        inputDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "class") {
                val relativePath = file.relativeTo(inputDir).path
                file.inputStream().use { inputStream ->
                    val classReader = ClassReader(inputStream)
                    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                    val classVisitor = ClassNameClassVisitor(
                        Opcodes.ASM9,
                        classWriter,
                        ObfuscatorMapping(classMapping.get())
                    )
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                    jarOutput.writeEntity(
                        relativePath,
                        classWriter.toByteArray()
                    )
                }
            } else if (file.isFile) {
                file.inputStream().use { inputStream ->
                    jarOutput.writeEntity(
                        file.relativeTo(inputDir).path,
                        inputStream
                    )
                }
            }
        }
    }

}
