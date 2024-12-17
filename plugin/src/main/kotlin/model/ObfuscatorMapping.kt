package com.kotlin.model

import com.kotlin.util.logDebug
import com.kotlin.util.toJson
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import java.io.File

/**
 * Created by DengLongFei
 * 2024/11/25
 */
class ObfuscatorMapping(
    private val classMapping: Map<String, String> = mapOf()
) {

    /**
     * 混淆文件路径
     */
    fun obfuscatorFilePath(name: String): String {
        val list = name.split(".")
        val suffix = list.lastOrNull() ?: ""
        val path = list.firstOrNull() ?: ""
        val newFile = classMapping[path.replace(File.separator, "/")]
        return if (newFile.isNullOrEmpty()) {
            name
        } else {
            newFile.replace("/", File.separator) + "." + suffix
        }
    }


    /**
     * 混淆类名
     */
    fun obfuscatorName(name: String?): String? {
        return obfuscatorDescriptorOrName(name)
    }

    /**
     * 混淆类型描述
     */
    fun obfuscatorDescriptor(name: String?): String? {
        return obfuscatorDescriptorOrName(name)
    }



    /**
     * 混淆类型类型签名
     * Landroid/widget/ArrayAdapter<Ljava/lang/CharSequence;>;
     * Ljava/util/WeakHashMap<Landroid/view/View;Ljava/lang/ref/WeakReference<*>;>;
     * Landroidx/databinding/adapters/ObservableListAdapter$1;
     * <V:Landroidx/compose/animation/core/AnimationVector;>Ljava/lang/Object;
     * <V:Landroidx/compose/animation/core/AnimationVector;>Ljava/lang/Object;
     * Landroidx/compose/animation/core/Animatable<Landroidx/compose/ui/geometry/Offset;Landroidx/compose/animation/core/AnimationVector2D;>;
     */
    fun obfuscatorSignature(signature: String?): String? {
      return  obfuscatorDescriptorOrName(signature)
    }

    /**
     * 混淆类型
     */
    fun obfuscatorType(type: Type): Type {
        return type.toObfuscatorType()
    }

    /**
     *混淆Handle
     */
    fun obfuscatorHandle(handle: Handle?): Handle? {
        handle ?: return null
        return Handle(
            handle.tag,
            obfuscatorName(handle.owner),
            handle.name,
            obfuscatorDescriptor(handle.desc),
            handle.isInterface
        )
    }

    /**
     *混淆ConstantDynamic
     */
    private fun obfuscatorConstantDynamic(value: ConstantDynamic) {
        val list = arrayListOf<Any?>()
        repeat(value.bootstrapMethodArgumentCount) { index ->
            val it = value.getBootstrapMethodArgument(index)
            list.add(obfuscatorBootstrapMethodArgumentsItem(it))
        }
        val newBootstrapMethodArguments = list.toTypedArray()
        ConstantDynamic(
            value.name,
            obfuscatorDescriptor(value.descriptor),
            value.bootstrapMethod?.let {
                obfuscatorHandle(it)
            },
            *newBootstrapMethodArguments
        )
    }


    fun obfuscatorBootstrapMethodArgumentsItem(it: Any?): Any? {
        return when (it) {
            is Type -> {
                obfuscatorType(it)
            }

            is Handle -> {
                obfuscatorHandle(it)
            }

            is ConstantDynamic -> {
                obfuscatorConstantDynamic(it)
            }

            else -> it
        }
    }

    /**
     * 获取混淆后类名
     */
    private fun String?.toObfuscatorName(): String? {
        val name = this
        name ?: return null
        return if (name.contains("$")) {
            name.split("$").joinToString("$") {
                classMapping[it] ?: it
            }
        } else {
            classMapping[name] ?: name
        }.also {
            logDebug("-----------getObfuscatorName  old  $name    new $it")
        }

    }

    /**
     * 获取混淆后描述符
     */
    private fun String?.toObfuscatorDescriptor(): String? {
        val descriptor = this
        descriptor ?: return null
        val regex = Regex("L([a-zA-Z0-9_/]+/)([a-zA-Z0-9_$]+)")
        return regex.replace(descriptor) { matchResult ->
            val path = matchResult.groups[1]?.value // 捕获路径部分
            val className = matchResult.groups[2]?.value // 捕获类名部分
            val replacedClass = (path+className).toObfuscatorName()
            "L$replacedClass" // 返回替换后的类描述符
        }
    }


    /**
     * 混淆类名或者描述符
     */
    @Synchronized
    fun obfuscatorDescriptorOrName(name: String?): String? {
        name ?: return null
        return if (name.startsWith("L") || name.startsWith("(") || name.startsWith("[") ||name.startsWith("<")) {
            name.toObfuscatorDescriptor()
        } else {
            name.toObfuscatorName()
        }
    }


    /**
     * 修改类型
     */
    private fun Type.toObfuscatorType(): Type {
        return when (this.sort) {
            Type.METHOD -> {
                // 方法类型，需要解析参数和返回类型
                val methodType = Type.getMethodType(this.descriptor)
                val argumentTypes = methodType.argumentTypes
                val returnType = methodType.returnType
                logDebug("-----argumentTypes " + argumentTypes.toJson())
                logDebug("-----returnType " + returnType.toJson())
                // 替换参数和返回类型
                val newArgumentTypes = argumentTypes.map { type ->
                    type.toObfuscatorType()
                }.toTypedArray()
                val newReturnType = returnType.toObfuscatorType()
                // 使用新参数类型和返回类型构造新的方法类型
                Type.getMethodType(newReturnType, *newArgumentTypes)
            }

            Type.OBJECT, 12 -> {
                val newClassName = this.className.replace('.', '/').toObfuscatorName()
                return Type.getObjectType(newClassName)
            }

            Type.ARRAY -> {
                val elementType = this.elementType
                val newElementType = elementType.toObfuscatorType()
                return Type.getType("[".repeat(this.dimensions) + newElementType.descriptor)
            }

            else -> {
                this
            }
        }
    }

    /**
     * 混淆类文件名称
     */
    fun obfuscatorFileSourceName(source: String?): String? {
        source ?: return null
        val list = source.split(".")

        val fileName = list.firstOrNull() ?: ""
        val suffixName = list.lastOrNull() ?: ""

        val newFile = fileNameMap[fileName]
        return if (newFile.isNullOrEmpty()) {
            return source
        } else {
            "$newFile.$suffixName"
        }

    }

    private val fileNameMap by lazy {
        classMapping.asSequence().map {
            it.key.split("/").lastOrNull() to it.value.split("/").lastOrNull()
        }.toMap()
    }

}