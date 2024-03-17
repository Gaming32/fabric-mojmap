plugins {
    java
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.shadowJar {
    val libs = "io.github.gaming32.fabricmojmap.libs"
    relocate("com.google.gson.", "$libs.gson.")
}
