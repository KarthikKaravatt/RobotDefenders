plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("io.gitlab.arturbosch.detekt").version("1.23.1")
    id("org.openjfx.javafxplugin").version("0.0.13")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(20)
}

application {
    mainClass.set("MainKt")
}
javafx {
    version = "20"
    modules("javafx.controls")
}