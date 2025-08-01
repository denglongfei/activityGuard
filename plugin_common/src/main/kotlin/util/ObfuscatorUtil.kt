package util

import com.kotlin.model.ClassInfo

/**
 * Created by DengLongFei
 * 2024/12/16
 */
class ObfuscatorUtil(
    private val split: String,//类名分隔符
    private val classNameCharPool: String, //类名字符
    private val dirNameCharPool: String, //包名字符
    private val outObfuscatedDir: String //输出目录
) {
//    private val split: String = "."
//    private val dirNameCharPool = "abcdefghijklmnopqrstuvwxyz"
//    private val classNameCharPool = "abcdefghijklmnopqrstuvwxyz0123456789"

    // 目录 和 混淆后目录
    var dirMapping = LinkedHashMap<String, String>()
        private set

    //存在的混淆目录名称
    private var dirValueSet = hashSetOf<String>()


    // 原目录名称为key  对应下的类名和混淆（不包含目录的
    private val dirAndClassList = LinkedHashMap<String, LinkedHashMap<String, String>>()

    fun initMap(
        classMapping: LinkedHashMap<String, ClassInfo>,
        dirMapping: LinkedHashMap<String, String>
    ) {
        this.dirMapping = dirMapping
        dirValueSet = dirMapping.values.toHashSet()
        classMapping.forEach {
            val (dir, name) = getClassDirAndName(it.key, split)
            val (obfuscatorDir, obfuscatorName) = getClassDirAndName(
                it.value.obfuscatorClassName,
                split
            )
            dirAndClassList.getOrPut(dir) { LinkedHashMap() }[name] = obfuscatorName
            dirMapping[dir] = obfuscatorDir
            dirValueSet.add(obfuscatorDir)
        }
    }

    fun initMap(
        classMapping: LinkedHashMap<String, String>
    ) {
        classMapping.forEach {
            val (dir, name) = getClassDirAndName(it.key, split = split)
            val (obfuscatorDir, obfuscatorName) = getClassDirAndName(it.value, split = split)
            dirAndClassList.getOrPut(dir) { LinkedHashMap() }[name] = obfuscatorName
            dirMapping[dir] = obfuscatorDir
            dirValueSet.add(obfuscatorDir)
        }
    }

    /**
     *  com.activityGuard.a to a.b
     */
    fun getObfuscatedClassName(name: String): String {
        val (dir, name) = getClassDirAndName(name, split)
        val newDir = dirMapping[dir] ?: let {
            val tem = generateDirName(dir)
            addDirMappingItem(dir, tem)
            tem
        }
        val newName = generateClassName(dir, name)
        dirAndClassList.getOrPut(dir) { linkedMapOf() }[name] = newName
        return if (newDir.isNotEmpty()) {
            "$newDir$split$newName"
        } else {
            "$newName"
        }
    }

    private var dirOffset = 0

    private fun generateDirName(dir: String): String {
        while (true) {
            val dirName = "$outObfuscatedDir$split" + generateName(
                dirMapping.size + dirOffset,
                minLength = 2,
                charset = dirNameCharPool
            )
            //存在混淆相同的名称
            if (dirValueSet.contains(dirName)) {
                dirOffset++
            } else {
                return dirName
            }
        }
    }

    private fun generateClassName(dir: String, name: String): String {
        val size = dirAndClassList.getOrPut(dir) { linkedMapOf() }.size
        return generateName(size, minLength = 1, charset = classNameCharPool)
    }


    private fun addDirMappingItem(dir: String, tem: String) {
        dirMapping[dir] = tem
        dirValueSet.add(tem)
        dirAndClassList.getOrPut(dir) { linkedMapOf() }
    }


    private fun generateName(counter: Int, minLength: Int, charset: String): String {
        val sb = StringBuilder()
        var value = counter
        value += (minLength - 1) * charset.length

        val firstCharIndex = value % charset.length
        val char = charset[firstCharIndex]
        if (char.isJavaIdentifierStart()) {
            sb.append(char)
        } else {
            sb.append("c_$char")
        }
        value /= charset.length
        while (value > 0) {
            val charIndex = value % charset.length
            sb.append(charset[charIndex])
            value /= charset.length
        }
        return sb.toString()
    }

}



