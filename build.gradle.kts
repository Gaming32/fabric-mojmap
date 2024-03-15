plugins {
    java
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

group = "io.github.gaming32"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
}

dependencies {
    compileOnly("net.fabricmc:fabric-loader:0.15.7")
    compileOnly("org.ow2.asm:asm-commons:9.6")

    compileOnly("org.jetbrains:annotations:24.1.0")

    implementation("net.fabricmc:mapping-io:0.5.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("net.lenni0451:Reflect:1.3.2")
}

tasks.compileJava {
    options.release = 8
}

tasks.shadowJar {
    manifest {
        attributes["Premain-Class"] = "io.github.gaming32.fabricmojmap.FabricMojmap"
    }

    rename("RuntimeModRemapper(\\$?.*?)\\.class", "RuntimeModRemapper$1.class.bin")

    val libs = "io.github.gaming32.fabricmojmap.libs"
    relocate("net.fabricmc.mappingio", "$libs.mappingio")
    relocate("com.google.gson", "$libs.gson")
    relocate("org.objectweb.asm", "$libs.asm")
    relocate("net.lenni0451.reflect", "$libs.reflect")
}

tasks.build.get().dependsOn(tasks.shadowJar)
