plugins {
    kotlin("jvm") version "2.3.21" apply false
}

group = "kr.hahaha98757"
version = "1.0-SNAPSHOT"

subprojects {
    pluginManager.apply("org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }
}