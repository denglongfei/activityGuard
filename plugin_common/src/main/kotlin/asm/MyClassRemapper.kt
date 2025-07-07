package com.kotlin.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper

/**
 * Created by DengLongFei
 * 2025/07/07
 */
class MyClassRemapper(
    classVisitor: ClassVisitor, remapper: Remapper
) : ClassRemapper(classVisitor, remapper) {
    override fun visitSource(source: String?, debug: String?) {
        super.visitSource(null, debug)
    }
}