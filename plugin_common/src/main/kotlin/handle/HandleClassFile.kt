package com.kotlin.handle

import com.kotlin.asm.AsmReMapper
import com.kotlin.asm.ClassNameClassVisitor
import com.kotlin.asm.MyClassRemapper
import com.kotlin.model.ActivityGuardExtension
import com.kotlin.model.ObfuscatorMapping
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import util.ObfuscatorUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Created by DengLongFei
 * 2024/12/26
 */
class HandleClassFile(
    private val allJars: MutableList<RegularFile>,
    private val allDirectories: MutableList<Directory>,
    private val outputFile: File,
    private val classMapping: LinkedHashMap<String, String>,
    private val actGuard: ActivityGuardExtension,
) {



    private val jarPaths = mutableSetOf<String>()

    private lateinit var obfuscatorUtil:ObfuscatorUtil
    /**
     * 修改class文件
     */
    fun chaneClassFile(obfuscator: ObfuscatorUtil) {
        println("----------" + outputFile.absolutePath)
        obfuscatorUtil = obfuscator
        obfuscatorUtil.initMap(classMapping)
        val jarOutputStream = JarOutputStream(
            BufferedOutputStream(
                FileOutputStream(
                    outputFile
                )
            )
        )
        // 处理 JAR 文件
        allJars.forEach { file ->
            processJarWithASM(file.asFile, jarOutputStream)
        }

        // 处理目录
        allDirectories.forEach { dir ->
            processDirectoryWithASM(dir.asFile, jarOutputStream)
        }
        jarOutputStream.close()


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
                    val classVisitor = MyClassRemapper(
                        classWriter,
                        AsmReMapper(classMapping,actGuard,obfuscatorUtil)
                    )
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                    jarOutput.writeEntity(jarEntry.name, classWriter.toByteArray())


//                    // 对类文件应用 ASM 处理
//                    val classReader = ClassReader(it)
//                    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
//                    val classVisitor = ClassNameClassVisitor(
//                        Opcodes.ASM9,
//                        classWriter,
//                        ObfuscatorMapping(classMapping)
//                    )
//                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
//                    jarOutput.writeEntity(jarEntry.name, classWriter.toByteArray())
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
                    val classVisitor = MyClassRemapper(
                        classWriter,
                        AsmReMapper(classMapping,actGuard,obfuscatorUtil)
                    )
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                    jarOutput.writeEntity(
                        relativePath,
                        classWriter.toByteArray()
                    )


//                    val classReader = ClassReader(inputStream)
//                    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
//                    val classVisitor = ClassNameClassVisitor(
//                        Opcodes.ASM9,
//                        classWriter,
//                        ObfuscatorMapping(classMapping)
//                    )
//                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
//                    jarOutput.writeEntity(
//                        relativePath,
//                        classWriter.toByteArray()
//                    )
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