plugins {
    id("java-library")
    kotlin("jvm")
}


dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(gradleKotlinDsl())
    compileOnly("com.android.tools.build:gradle:8.6.0")
    compileOnly("com.android.tools:common:31.6.0")
    implementation(project(":plugin_common"))
}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

apply(from = "../maven_local.gradle")






