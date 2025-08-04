package com.kotlin.asm

import com.kotlin.model.ObfuscatorMapping
import util.logDebug
import util.toJson
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.ModuleVisitor
import org.objectweb.asm.RecordComponentVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.TypePath
import util.isDebug

/**
 * Created by DengLongFei
 * 2024/11/18
 */

class ClassNameClassVisitor(
    val apiVersion: Int,
    cv: ClassVisitor,
    val obfuscatorMapping: ObfuscatorMapping,
) : ClassVisitor(apiVersion, cv) {

    /**
     * 处理注解
     */
    private fun handleAnnotation(
        annotationVisitor: AnnotationVisitor?,
    ): AnnotationVisitor? {
        annotationVisitor ?: return null
        return object : AnnotationVisitor(apiVersion, annotationVisitor) {
            override fun visit(name: String?, value: Any?) {
                logDebug(
                    "----------visitAnnotation visit1  name $name    " + value?.javaClass?.name
                )
                when (value) {
                    is String -> {
                        super.visit(name, obfuscatorMapping.obfuscatorDescriptor(value))
                    }

                    is Type -> {
                        super.visit(name, obfuscatorMapping.obfuscatorType(value))
                    }

                    else -> {
                        super.visit(name, obfuscatorMapping.obfuscatorBootstrapMethodArgumentsItem(value))
                    }
                }
            }

            override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                logDebug("----------visitAnnotation visitEnum  name $name descriptor $descriptor value $value")
                super.visitEnum(name, obfuscatorMapping.obfuscatorDescriptor(descriptor), value)
            }

            override fun visitArray(name: String?): AnnotationVisitor? {
                logDebug("----------visitAnnotation visitArray  name $name")
                return handleAnnotation(super.visitArray(name))
            }
        }
    }


    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        logDebug("----------visit  version " + version + "   " + " access " + access + "    name " + name + "   " + " signature " + signature + "   " + " superName " + superName + "   " + " interfaces " + interfaces?.joinToString { it })
        super.visit(
            version,
            access,
            obfuscatorMapping.obfuscatorName(name),
            obfuscatorMapping.obfuscatorSignature(signature),
            obfuscatorMapping.obfuscatorName(superName),
            interfaces?.map { obfuscatorMapping.obfuscatorName(it) }?.toTypedArray()
        )
    }


    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        logDebug("----------visitAnnotation  descriptor $descriptor    visible $visible")
        return handleAnnotation(
            super.visitAnnotation(
                obfuscatorMapping.obfuscatorDescriptor(
                    descriptor
                ), visible
            )
        )
    }

    override fun visitAttribute(attribute: Attribute?) {
        logDebug("----------visitAttribute  attribute ${attribute.toJson()} ")
        super.visitAttribute(attribute)
    }

    override fun visitField(
        access: Int, name: String?, descriptor: String?, signature: String?, value: Any?
    ): FieldVisitor {
        logDebug("----------visitField  access $access    name $name    descriptor $descriptor    signature $signature    value $value")
        return super.visitField(
            access,
            name,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            obfuscatorMapping.obfuscatorSignature(signature),
            value
        )
    }


    override fun visitInnerClass(
        name: String?, outerName: String?, innerName: String?, access: Int
    ) {
        logDebug(buildString {
            append("----------visitInnerClass ")
            append(" name ")
            append(name)
            append("   ")
            append(" outerName ")
            append(outerName)
            append("   ")
            append(" innerName ")
            append(innerName)
            append("   ")
            append(" access ")
            append(access)
        })
        super.visitInnerClass(
            obfuscatorMapping.obfuscatorName(name),
            obfuscatorMapping.obfuscatorName(outerName),
            obfuscatorMapping.obfuscatorName(innerName),
            access
        )
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        logDebug("----------visitMethod  access $access    name $name    descriptor $descriptor    signature $signature    exceptions $exceptions")
        val methodVisitor = super.visitMethod(
            access,
            name,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            obfuscatorMapping.obfuscatorSignature(signature),
            exceptions
        )
        return ClassNameMethodVisitor(apiVersion, methodVisitor, obfuscatorMapping)
    }


    override fun visitModule(name: String?, access: Int, version: String?): ModuleVisitor {
        logDebug("----------visitModule  name $name   access $access   version $version  ")
        return super.visitModule(name, access, version)
    }

    override fun visitNestHost(nestHost: String?) {
        logDebug("----------visitNestHost  nestHost $nestHost ")
        super.visitNestHost(nestHost)
    }

    override fun visitNestMember(nestMember: String?) {
        logDebug("----------nestMember  nestMember $nestMember ")
        super.visitNestMember(nestMember)
    }

    override fun visitOuterClass(owner: String?, name: String?, descriptor: String?) {
        logDebug("----------visitOuterClass  owner $owner    name $name    descriptor $descriptor   ")
        super.visitOuterClass(
            obfuscatorMapping.obfuscatorName(owner),
            obfuscatorMapping.obfuscatorName(name),
            obfuscatorMapping.obfuscatorDescriptor(descriptor)
        )
    }

    override fun visitPermittedSubclass(permittedSubclass: String?) {
        logDebug("----------permittedSubclass  permittedSubclass $permittedSubclass ")
        super.visitPermittedSubclass(permittedSubclass)
    }

    override fun visitRecordComponent(
        name: String?,
        descriptor: String?,
        signature: String?
    ): RecordComponentVisitor {
        logDebug("----------visitRecordComponent  name $name descriptor $descriptor signature $signature ")
        return super.visitRecordComponent(
            name,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            obfuscatorMapping.obfuscatorSignature(signature)
        )
    }

    override fun visitSource(source: String?, debug: String?) {
        val newFilName = obfuscatorMapping.obfuscatorFileSourceName(source)
        logDebug("----------visitSource  source $source   new $newFilName   debug $debug  ")
        super.visitSource(newFilName, debug)
    }

    override fun visitTypeAnnotation(
        typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean
    ): AnnotationVisitor {
        logDebug("----------visitTypeAnnotation  typeRef $typeRef    typePath $typePath    descriptor $descriptor    visible $visible")
        return super.visitTypeAnnotation(
            typeRef,
            typePath,
            obfuscatorMapping.obfuscatorDescriptor(descriptor),
            visible
        )
    }

}
