plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
}

group = "dev.balakumar"
version = "1.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3.3")
    type.set("IC")
    plugins.set(listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin",
        "JUnit"
    ))
    downloadSources.set(false)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    patchPluginXml {
        sinceBuild.set("231") // Beginning of 2023.1
        untilBuild.set("243.*") // Up to 2024.3
    }
    test {
        exclude("**/*")
    }
}