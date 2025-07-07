package com.kotlin.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.kotlin.model.ObfuscatorMapping
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

/**
 * Created by DengLongFei
 * 2024/11/18
 */
abstract class ClassNameTransform :
    AsmClassVisitorFactory<ClassNameTransform.ClassNameTransformParams> {
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return ClassNameClassVisitor(
            instrumentationContext.apiVersion.get(),
            nextClassVisitor,
            ObfuscatorMapping(parameters.get().classMapping.get())
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return true
    }


    interface ClassNameTransformParams : InstrumentationParameters {
        @get:Input
        val classMapping: MapProperty<String, String>
    }


}

