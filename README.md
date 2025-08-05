# activityGuard：Android activityObfuscator
**activityGuard** 是一种针对四大组件进行混淆的解决方案，能够在打包时对apk和aab中的Activity、Service、Application和自定义的view进行名称混淆以提升应用的安全性。

### 目的
*   **防止逆向：** Android四大组件的类名直接暴露在 AndroidManifest.xml 和代码中,容易被反编译后根据名称了解应用逻辑。
*   **增强安全性：** 混淆名称增加了攻击者定位关键组件的难度，降低被针对性攻击的风险。
*   **马甲包：** 降低aab包查重率，避免上架Google Play因查重率过高，导致下架或封号问题

### 原理分析
Android四大组件在打包过程中不能够R8被混淆，因为组件在AndroidManifest.xml以明文形式存在Android系统通过反射创建相关类来启动。所以我们需要在R8执行前修改AndroidManifest.xml和layout布局中的类名，并把新的名称keep的R8的混淆规则中（R8混淆执行时是以keep住的类为节点，如果没引用的类会被移除掉）。最后在通过asm字节码修改类名，就能够实现对四大组件和自定义view实现混淆名称了。
activityGuard 通过自定义Gradle任务在打包过程中修改替换AndroidManifest.xml和layout中的类名和class的类名来实现对Android四大组件的混淆

博客 地址[：](url)<https://juejin.cn/post/7449723991638327296>
### 使用方法

插件基于Gradle8.0，并且因为基于aapt2生成的aapt_rules.txt来混淆类名，所以项目需要开启  **isMinifyEnabled = true**

每次混淆会在当前项目下生成对应的**mapping.txt**记录对应混淆类，插件默认会更加mapping.txt文件增量混淆名称，所以当需要不同混淆名时，可以删除mapping.txt文件或者自己实现对应生成规则方法（自己生成时记得确保唯一性）

```
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath "com.github.denglongfei:activityGuard:1.3.0"
    }
}
```
配置
```
plugins {
    id("activityGuard")
}
//以下均为非必须
//actGuard {
//    //是否开启
//    isEnable = true
//    //不需要混淆的类
//    whiteClassList = hashSetOf(
//        "com.activityGuard.model.Bean",
//    )
//    //额外需要混淆的类
//    otherClassList = hashSetOf(
//        "com.activityGuard.*",
//
//        )
//    //额外需要混淆 需要修改包名的类
//    otherChangePackageList = hashSetOf(
//        "com.activityGuard.vm.*",
//    )
//    //类名混淆字符串
//     classNameCharPool = "abcdefghijklmnopqrstuvwxyz0123456789"
//
//    //包名混淆字符串
//     dirNameCharPool = "abcdefghijklmnopqrstuvwxyz"
//
//    //混淆后都会输出在这个目录下
//    outObfuscatedDir = "guard"
//}


```

#### AAB混淆

aab打包混淆流程图如下
![aab](https://github.com/user-attachments/assets/b13c1c3c-afb5-4870-b32b-ee6293bb97c5)

#### Apk混淆

apk打包混淆流程图如下

![apk](https://github.com/user-attachments/assets/86b16d1d-e0f1-45d7-9b12-ab2bcea33d06)

### 最终效果

**apk**
![image](https://github.com/user-attachments/assets/87fd3529-d204-4db9-9d21-82f616951efb)
![image](https://github.com/user-attachments/assets/c122421a-19eb-470f-a5ef-b77b8b21012d)

**aab**

![image](https://github.com/user-attachments/assets/e65ca13c-0101-482c-9e70-620cb6f0ab6a)
![image](https://github.com/user-attachments/assets/13bc4dfb-11ab-4607-b90a-834a59ad8bf3)



