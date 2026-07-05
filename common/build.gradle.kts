import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm")
}

description = "common"

dependencies {
    testImplementation(platform(SpringBootPlugin.BOM_COORDINATES))
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation(kotlin("test"))
}
