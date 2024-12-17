package com.kotlin.util

import com.android.aapt.Resources
import com.android.aapt.Resources.XmlAttribute
import com.kotlin.model.ClassInfo
import java.util.zip.ZipFile

/**
 * Created by DengLongFei
 * 2024/11/19
 */


fun readByte(zipFile: ZipFile, path: String): ByteArray {
    val zipEntry = zipFile.getEntry(path)
    val bytes = zipFile.getInputStream(zipEntry).use { it.readBytes() }
    return bytes
}

/**
 * 修改xml 中的activity名称
 */
fun changeManifestActivityName(
    xmlNode: Resources.XmlNode,
    classMapping: Map<String, ClassInfo>
): Resources.XmlNode {
    return changeXmlNodeAttribute(xmlNode, "activity", "name", classMapping)
}

/**
 * 修改属性 属性名称
 */
fun changeXmlNodeAttribute(
    xmlNode: Resources.XmlNode,
    xmlAttributeName: String,
    xmlAttributeValue: String,
    classMapping: Map<String, ClassInfo>
): Resources.XmlNode {
    val xmlNodeBuilder = xmlNode.toBuilder()
    val elementXmlElement = xmlNodeBuilder.element.toBuilder()
    xmlNodeBuilder.element.childList.forEachIndexed { index, xmlNodeChild ->
        when {
            xmlNodeChild.element.name.isEmpty() -> {
                elementXmlElement.setChild(index, xmlNodeChild)
            }

            xmlNodeChild.element.name == xmlAttributeName -> {
                val item = changeXmlAttributeList(xmlNodeChild, xmlAttributeValue, classMapping)
                elementXmlElement.setChild(index, item)
            }

            else -> {
                val newXmlNode = changeXmlNodeAttribute(
                    xmlNodeChild,
                    xmlAttributeName,
                    xmlAttributeValue,
                    classMapping
                )
                elementXmlElement.setChild(index, newXmlNode)
            }
        }
    }
    xmlNodeBuilder.setElement(elementXmlElement)
    return xmlNodeBuilder.build()
}

fun changeXmlAttributeList(
    xmlNode: Resources.XmlNode,
    xmlAttributeValue: String,
    classMapping: Map<String, ClassInfo>
): Resources.XmlNode {
    val xmlNodeBuilder = xmlNode.toBuilder()
    val elementXmlNodeBuilder = xmlNodeBuilder.element.toBuilder()
    xmlNodeBuilder.element.attributeList.forEachIndexed { index, xmlAttribute ->
        if (xmlAttribute.name == xmlAttributeValue) {
            val name = xmlAttribute.value
            val classInfo = classMapping[name]
            if (classInfo != null) {
                classInfo.isUse = true
                elementXmlNodeBuilder.setAttribute(
                    index,
                    changeXmlAttribute(xmlAttribute, classInfo.obfuscatorClassName)
                )
            } else {
                elementXmlNodeBuilder.setAttribute(index, xmlAttribute)
            }
        } else {
            elementXmlNodeBuilder.setAttribute(index, xmlAttribute)
        }

    }
    xmlNodeBuilder.setElement(elementXmlNodeBuilder)
    return xmlNodeBuilder.build()
}


/**
 * 修改节点属性值
 */
private fun changeXmlAttribute(xmlAttribute: XmlAttribute, obfuscatorName: String): XmlAttribute {
    val xmlAttributeBuilder = xmlAttribute.toBuilder()
    xmlAttributeBuilder.value = obfuscatorName
    return xmlAttributeBuilder.build()
}


/**
 * 修改layout 中的名称
 */
fun changeLayoutXmlName(
    bundleZip: ZipFile,
    path: String,
    classMapping: Map<String, ClassInfo>
): Resources.XmlNode {
    val xmlNode = Resources.XmlNode.parseFrom(readByte(bundleZip, path))
    return changeXmlNodeName(xmlNode, classMapping)
}


private fun changeXmlNodeName(
    xmlNode: Resources.XmlNode,
    classMapping: Map<String, ClassInfo>
): Resources.XmlNode {
    val xmlNodeBuilder = xmlNode.toBuilder()

    //修改当前节点名称
    val name = xmlNodeBuilder.element.name
    classMapping[name]?.also { classInfo ->
        classInfo.isUse = true
        xmlNodeBuilder.setElement(changeXmlElement(xmlNodeBuilder.element, classInfo.obfuscatorClassName))
    }

    //修改子节点名称
    val elementXmlElement = xmlNodeBuilder.element.toBuilder()
    xmlNodeBuilder.element.childList.forEachIndexed { index, xmlNodeChild ->
        elementXmlElement.setChild(index, changeXmlNodeName(xmlNodeChild, classMapping))
    }
    xmlNodeBuilder.setElement(elementXmlElement)

    return xmlNodeBuilder.build()
}

/**
 * 修改节点名称
 */
private fun changeXmlElement(
    xmlElement: Resources.XmlElement,
    obfuscatorName: String
): Resources.XmlElement {
    val xmlElementBuilder = xmlElement.toBuilder()
    xmlElementBuilder.name = obfuscatorName
    return xmlElementBuilder.build()
}