plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}


dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(gradleKotlinDsl())
    compileOnly("com.android.tools:common:31.6.0")
    compileOnly("com.android.tools:sdk-common:31.1.4")
    testImplementation("junit:junit:4.13")
    implementation(project(":plugin_common"))

    implementation(project(":plugin_agp_v74"))
    implementation(project(":plugin_agp_v86"))
    implementation(project(":plugin_agp_v87"))

}





java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

apply(from = "../maven_local.gradle")


val mGroup = project.property("mGroup").toString()
val mId = project.property("mId").toString()
val mVersion = project.property("mVersion").toString()


gradlePlugin {
    plugins {
        create("actGuardPlugin") {
            id = mId
            group = mGroup
            version = mVersion
            implementationClass = "com.kotlin.ActivityGuardPlugin"
        }
    }
}







