plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
}


buildscript {
    val mVersion = project.property("mVersion").toString()
    dependencies {
        classpath("com.github.denglongfei:activityGuard:$mVersion")
    }
}
