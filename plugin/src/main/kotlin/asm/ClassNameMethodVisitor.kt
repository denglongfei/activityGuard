package com.kotlin.asm

import com.kotlin.model.ObfuscatorMapping
import com.kotlin.util.logDebug
import com.kotlin.util.toJson
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.TypePath
import kotlin.math.log

/**
 * Created by DengLongFei
 * 2024/11/25
 * 处理方法
 */
class ClassNameMethodVisitor(
    apiVersion: Int,
    methodVisitor: MethodVisitor,
    private val obfuscatorMapping: ObfuscatorMapping,
) : MethodVisitor(apiVersion, methodVisitor) {

    /**
     * 修改引导方法参数
     */
    private fun obfuscatorBootstrapMethodArguments(bootstrapMethodArguments: Array<out Any?>): Array<Any?> {
        val newBootstrapMethodArguments = bootstrapMethodArguments.map {
            obfuscatorMapping.obfuscatorBootstrapMethodArgumentsItem(it)
        }.toTypedArray()
        return newBootstrapMethodArguments
    }


    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
        logDebug("----------visitAnnotation MethodVisitor descriptor $descriptor  visible $visible")
        return super.visitAnnotation(obfuscatorMapping.obfuscatorDescriptor(descriptor), visible)
    }

    override fun visitAttribute(attribute: Attribute?) {
        logDebug("----------visitAttribute attribute  ${attribute.toJson()}  ")
        super.visitAttribute(attribute)
    }


    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        logDebug("----------visitFieldInn  opcode $opcode    owner $owner    name $name    descriptor $descriptor")
        super.visitFieldInsn(
            opcode,
            obfuscatorMapping.obfuscatorName(owner),
            name,
            obfuscatorMapping.obfuscatorDescriptor(descriptor)
        )
    }

    override fun visitFrame(
        type: Int,
        numLocal: Int,
        local: Array<out Any>?,
        numStack: Int,
        stack: Array<out Any>?
    ) {
        logDebug("----------visitFrame " + " type " + type + "   " + " numLocal " + numLocal + "   " + " local " + local.toJson() + "   " + " numStack " + numStack + "   " + " stack " + stack.toJson() + "   ")
        val newLocal = local?.map {
            if (it is String) {
                obfuscatorMapping.obfuscatorName(it)
            } else {
                it
            }
        }?.toTypedArray()
        val newStack = stack?.map {
            if (it is String) {
                obfuscatorMapping.obfuscatorName(it)
            } else {
                it
            }
        }?.toTypedArray()
        super.visitFrame(type, numLocal, newLocal, numStack, newStack)
    }

    override fun visitInsnAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {
        logDebug("----------visitInsnAnnotation  typeRef $typeRef    typePath ${typePath.toJson()}    descriptor $descriptor    visible $visible")
        return super.visitInsnAnnotation(
            typeRef,
            typePath,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            visible
        )
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        logDebug("----------visitInvokeDynamicInsn  name " + name + "   " + " descriptor " + descriptor + "   " + " bootstrapMethodHandle " + bootstrapMethodHandle.toJson() + "   " + "\n bootstrapMethodArguments " + bootstrapMethodArguments.toJson())
        val newBootstrapMethodArguments =
            obfuscatorBootstrapMethodArguments(bootstrapMethodArguments)
        logDebug("----------visitInvokeDynamicInsn newBootstrapMethodArguments "+newBootstrapMethodArguments.toJson())
        super.visitInvokeDynamicInsn(
            name, obfuscatorMapping.obfuscatorDescriptor(descriptor),
            obfuscatorMapping.obfuscatorHandle(bootstrapMethodHandle), *newBootstrapMethodArguments
        )
    }

    override fun visitLabel(label: Label?) {
        logDebug("----------visitLabel  label $label")
        super.visitLabel(label)
    }

    override fun visitLdcInsn(value: Any?) {
        logDebug("----------visitLdcInsn " + " value " + value.toJson())
        val newValue = obfuscatorMapping.obfuscatorBootstrapMethodArgumentsItem(value)
        super.visitLdcInsn(newValue)
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int
    ) {
        logDebug(
            "----------visitLocalVariable  name $name    descriptor $descriptor    signature $signature    start $start end $end  index $index"
        )
        super.visitLocalVariable(
            name,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            obfuscatorMapping.obfuscatorSignature(signature),
            start,
            end,
            index
        )
    }

    override fun visitLocalVariableAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        start: Array<out Label>?,
        end: Array<out Label>?,
        index: IntArray?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {
        logDebug(
            "----------visitLocalVariableAnnotation  typeRef $typeRef    typePath $typePath   " +
                    " start $start    end $end index $index  descriptor $descriptor  visible $visible"
        )
        return super.visitLocalVariableAnnotation(
            typeRef,
            typePath,
            start,
            end,
            index,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            visible
        )
    }


    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        logDebug(
            "----------visitMethodInsn  opcode $opcode    " +
                    "owner $owner    name $name    descriptor $descriptor    isInterface $isInterface"
        )
        super.visitMethodInsn(
            opcode,
            obfuscatorMapping.obfuscatorDescriptorOrName(owner),
            name,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            isInterface
        )
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        logDebug(
            "----------visitMultiANewArrayInsn  descriptor $descriptor    numDimensions $numDimensions"
        )
        super.visitMultiANewArrayInsn(
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            numDimensions
        )
    }


    override fun visitParameterAnnotation(
        parameter: Int,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {
        logDebug(
            "----------parameter  descriptor $descriptor    visible $visible"
        )
        return super.visitParameterAnnotation(
            parameter,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            visible
        )
    }

    override fun visitTryCatchAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {
        logDebug(
            "----------visitTryCatchAnnotation  typeRef $typeRef    typePath $typePath  descriptor $descriptor $visible"
        )
        return super.visitTryCatchAnnotation(
            typeRef,
            typePath,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            visible
        )
    }


    override fun visitTryCatchBlock(
        start: Label?, end: Label?, handler: Label?, type: String?
    ) {
        logDebug("----------visitTryCatchBlock  start $start    end $end    handler $handler    type $type")
        super.visitTryCatchBlock(start, end, handler, obfuscatorMapping.obfuscatorDescriptor(type))
    }

    override fun visitTypeAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean
    ): AnnotationVisitor {
        logDebug("----------visitTypeAnnotation  typeRef $typeRef    typePath $typePath    descriptor $descriptor    visible $visible")
        return super.visitTypeAnnotation(
            typeRef, typePath,
            obfuscatorMapping.obfuscatorDescriptor(descriptor), visible
        )
    }


    override fun visitTypeInsn(opcode: Int, type: String?) {
        logDebug("----------visitTypeInsn  opcode $opcode    type $type")
        super.visitTypeInsn(opcode, obfuscatorMapping.obfuscatorName(type))

    }
}