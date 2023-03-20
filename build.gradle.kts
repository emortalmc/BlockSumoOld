import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"

    java
}

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://jitpack.io")
}

dependencies {
//    implementation("com.github.hollow-cube:Minestom:229117a186")
//    implementation("com.github.EmortalMC:Immortal:30f89199e2")
    implementation("dev.emortal.immortal:Immortal:3.0.1")
    implementation("com.github.hollow-cube:Minestom:e6d4a2cc91")
    implementation("com.github.EmortalMC:TNT:4ef1b53482")

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
