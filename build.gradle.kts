plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    application
}

group = "com.blum.bot"
version = "1.0"

val ktor_version: String by project

application {
    mainClass = "com.blum.bot.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-encoding:$ktor_version")
    implementation("org.slf4j:slf4j-simple:2.0.3")
}

kotlin {
    jvmToolchain(8)
}