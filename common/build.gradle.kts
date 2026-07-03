plugins {
    kotlin("jvm")
}

description = "common"

dependencies {
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    testImplementation(kotlin("test"))
}
