import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("com.github.johnrengelman.shadow") version("8.1.1")
    kotlin("jvm") version("1.9.23")
}

group = "io.github.gaming32"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    // Loader
    compileOnly("net.fabricmc:fabric-loader:0.15.7")

    // ASM (available with Loader)
    compileOnly("org.ow2.asm:asm:9.6")
    compileOnly("org.ow2.asm:asm-analysis:9.6")
    compileOnly("org.ow2.asm:asm-commons:9.6")
    compileOnly("org.ow2.asm:asm-tree:9.6")
    compileOnly("org.ow2.asm:asm-util:9.6")

    compileOnly("org.jetbrains:annotations:24.1.0")

    // Our deps
    implementation("net.fabricmc:access-widener:2.1.0")
    implementation("net.fabricmc:mapping-io:0.5.1")
    implementation("net.fabricmc:tiny-remapper:0.10.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.lenni0451:Reflect:1.3.2")
    implementation("maven.modrinth:mod-loading-screen:1.0.4:api")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
}

tasks.compileJava {
    options.release = 8
}

tasks.compileKotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}

tasks.shadowJar {
    manifest {
        attributes["Premain-Class"] = "io.github.gaming32.fabricmojmap.FabricMojmap"
    }

    dependencies {
        exclude(dependency("org.ow2.asm:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
    }

    filesMatching("net/fabricmc/loader/**") {
        path = path.replace("net/fabricmc/loader/", "io/github/gaming32/fabricmojmap/embedded/net/fabricmc/loader/")
    }

    val libs = "io.github.gaming32.fabricmojmap.libs"
    relocate("net.fabricmc.accesswidener.", "$libs.accesswidener.")
    relocate("net.fabricmc.mappingio.", "$libs.mappingio.")
    relocate("net.fabricmc.tinyremapper.", "$libs.tinyremapper.")
    relocate("com.google.gson.", "$libs.gson.")
    relocate("net.lenni0451.reflect.", "$libs.reflect.")
    relocate("io.github.gaming32.modloadingscreen.api.", "$libs.mlsapi.")
}

tasks.build.get().dependsOn(tasks.shadowJar)
