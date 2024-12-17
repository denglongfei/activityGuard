plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "1.9.0"
    java
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.5")
    implementation(kotlin("stdlib"))
    implementation(gradleKotlinDsl())
    implementation("org.ow2.asm:asm-util:9.7")
    implementation("com.google.protobuf:protobuf-java:3.21.12")
    implementation("com.google.guava:guava:27.0.1-jre")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation("junit:junit:4.13")
    compileOnly("com.android.tools.build:aapt2-proto:8.1.4-10154469")
    compileOnly("com.android.tools.build:bundletool:1.14.0")
    compileOnly("com.android.tools.build:gradle:8.1.4")
    implementation("com.android.tools:common:31.6.0")
    implementation("com.android.tools:sdk-common:31.1.4")
}

group = "com.github.denglongfei"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("actGuardPlugin") {
            id = "activityGuard"
            group = "com.github.denglongfei"
            description = "activityGuard"
            version = "1.0.0"
            implementationClass = "com.kotlin.ObfuscatorPlugin"
        }
    }
}


java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishing {
    publications {
        create("release", MavenPublication::class) {
            from(components["java"])
            groupId = "com.github.denglongfei"
            artifactId = "activityGuard"
            version = "1.0.0"
        }
        repositories {
            mavenLocal()
        }
    }
}









