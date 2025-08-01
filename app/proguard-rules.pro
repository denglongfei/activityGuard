# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

##指定压缩级别
#-optimizationpasses 5
#
##不跳过非公共的库的类成员
#-dontskipnonpubliclibraryclassmembers
#
##优化  不优化输入的类文件
#-dontoptimize
#
##预校验
#-dontpreverify
#
##混淆时是否记录日志
#-verbose

#-repackageclasses

#-obfuscationdictionary ../app/keywords.txt


-keep class * implements java.io.Serializable
