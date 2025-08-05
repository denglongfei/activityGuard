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
    private val outObfuscatedDir: String, //输出目录
    private val otherChangePackageList: HashSet<String> = hashSetOf(),// 额外需要混淆的类 需要修改包名
) {
//    private val split: String = "."
//    private val dirNameCharPool = "abcdefghijklmnopqrstuvwxyz"
//    private val classNameCharPool = "abcdefghijklmnopqrstuvwxyz0123456789"


    private val packagePattern by lazy {
        otherChangePackageList.toPatterns()
    }

    // 目录 和 混淆后目录
    var dirMapping = LinkedHashMap<String, String>()
        private set




    //存在的混淆目录名称
    private var dirValueSet = hashSetOf<String>()


    // 原目录名称为key  对应下的类名和混淆（不包含目录的
    private val dirAndClassList = LinkedHashMap<String, LinkedHashMap<String, String>>()


    //xml中的混淆类
    private val xmlDirMapping = LinkedHashMap<String, String>()
    private val xmlClassMapping = LinkedHashMap<String, String>()

    /**
     * xml 获取混淆类名使用
     */
    fun initMap(
        classMapping: LinkedHashMap<String, ClassInfo>,
        dirMapping: LinkedHashMap<String, String>
    ) {
        this.dirMapping = dirMapping
        dirValueSet = dirMapping.values.toHashSet()
        classMapping.forEach {
            val (dir, name) = getObfClassDirAndName(it.key)
            val (obfuscatorDir, obfuscatorName) = getObfClassDirAndName(it.value.obfuscatorClassName)
            dirAndClassList.getOrPut(dir) { LinkedHashMap() }[name] = obfuscatorName
            dirMapping[dir] = obfuscatorDir
            dirValueSet.add(obfuscatorDir)
        }
    }

    /**
     * 其他类
     */
    fun initMap(
        classMapping: LinkedHashMap<String, String>
    ) {
        classMapping.forEach {
            val (dir, name) = getObfClassDirAndName(it.key)
            val (obfuscatorDir, obfuscatorName) = getObfClassDirAndName(it.value)
            dirAndClassList.getOrPut(dir) { LinkedHashMap() }[name] = obfuscatorName
            dirMapping[dir] = obfuscatorDir
            dirValueSet.add(obfuscatorDir)

            xmlDirMapping[dir] = obfuscatorDir
            xmlClassMapping[it.key] = it.value
        }
    }

//    /**
//     *  com.activityGuard.a to a.b
//     */
//    fun getObfuscatedClassName(originalName: String): String {
//        val (dir, name) = getObfClassDirAndName(originalName)
//        if (dir.isEmpty()) {
//            return name
//        }
//        val newDir = dirMapping[dir] ?: let {
//            val tem = generateDirName(dir,originalName)
//            addDirMappingItem(dir, tem)
//            tem
//        }
//        val newName = generateClassName(dir, name)
//        dirAndClassList.getOrPut(dir) { linkedMapOf() }[name] = newName
//        return if (newDir.isNotEmpty()) {
//            "$newDir$split$newName"
//        } else {
//            "$newName"
//        }
//    }


    fun getObfuscatedClassName(originalName: String): String {
        val (dir, name) = getObfClassDirAndName(originalName)
        if (dir.isEmpty()) {
            return name
        }
        //xml存在当前类
        val obClassName = xmlClassMapping[originalName]
        if (obClassName != null) {
            return obClassName
        }

        //xml 存在这个目录 需要重新计算 根据条件判断是否需要
        val xmlDir = xmlDirMapping[dir]
        val newDir = if (xmlDir != null) {
            if (inRegex(packagePattern, originalName)) {
                //需要改变目录
                xmlDir
            }else{
                //不需要改变目录
                dir
            }
        } else {
            dirMapping[dir] ?: let {
                val tem = generateDirName(dir, originalName)
                //不混淆目录时
                if (tem == dir) {
                    tem
                } else {
                    addDirMappingItem(dir, tem)
                    tem
                }
            }
        }
        val newName = generateClassName(dir, name)
        dirAndClassList.getOrPut(dir) { linkedMapOf() }[name] = newName
        return if (newDir.isNotEmpty()) {
            "$newDir$split$newName"
        } else {
            "$newName"
        }
    }

    /**
     * 获取目录和类名
     */
    private fun getObfClassDirAndName(name: String): Pair<String, String> {
        return getClassDirAndName(name, split)
    }


    private var dirOffset = 0

    private fun generateDirName(dir: String, originalName: String): String {
        //xml中的类 需要混淆包
        if (split == ".") {
            return getDirName()
        }
        //其他类 满足规则
        if (inRegex(packagePattern, originalName)) {
            return getDirName()
        }
        return dir
    }

    private fun getDirName(): String {
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
        val nameMap = dirAndClassList.getOrPut(dir) { linkedMapOf() }
        val obName = nameMap[name]
        if (obName != null) {
            return obName
        } else {
            val size = nameMap.size
            return generateName(size, minLength = 1, charset = classNameCharPool)
        }
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



