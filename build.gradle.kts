import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.22"
    kotlin("plugin.serialization") version "1.7.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"

    java
}

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.Minestom:Minestom:eb06ba8664")
    implementation("dev.emortal.immortal:Immortal:3.0.1")
    implementation("dev.emortal.tnt:TNT:1.0.0")

    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        mergeServiceFiles()

        manifest {
            attributes (
                "Main-Class" to "dev.emortal.bs.BlockSumoMainKt",
                "Multi-Release" to true
            )
        }

        transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
    }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    build { dependsOn(shadowJar) }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}
