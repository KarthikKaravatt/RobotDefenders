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
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.1")
}

tasks.test {
    useJUnitPlatform()
}

detekt {
    toolVersion = "1.23.1"
    config.setFrom(file("default-detekt-config.yml"))
    buildUponDefaultConfig = true
}
kotlin {
    jvmToolchain(20)
}

application {
    mainClass.set("App")
}
javafx {
    version = "20"
    modules("javafx.controls")
}
