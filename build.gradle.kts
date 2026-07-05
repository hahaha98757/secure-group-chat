import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21" apply false
}

allprojects {
    group = "kr.hahaha98757.securegroupchat"
    this.version = project.findProperty("version") ?: throw GradleException("Version property not found in gradle.properties")
}

subprojects {
    pluginManager.apply("org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
    }

    tasks.named<Jar>("jar") { enabled = false }
}

val packageFolder = file("build/SecureGroupChat-$version")

tasks.register("build") {
    group = "build"
    description = "Builds the entire application and packages it into a single directory."
    if (packageFolder.exists()) packageFolder.deleteRecursively()
    packageFolder.mkdirs()
    dependsOn(":client:packageExe", ":server:packageExe")

    doLast {
        copy {
            from(file("client/build/jpackage"))
            into(packageFolder)
        }
        copy {
            from(file("server/build/jpackage"))
            into(packageFolder)
        }
    }
}