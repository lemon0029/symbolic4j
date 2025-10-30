plugins {
    kotlin("jvm") version "2.0.0"
}

group = "io.nullptr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.15.0")
    implementation("com.github.luben:zstd-jni:1.5.6-6")

    testImplementation(kotlin("test"))
}

sourceSets {
    sourceSets.main {
        java.srcDirs("src/main/kotlin")
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}