plugins {
    kotlin("jvm") version "1.9.23"
    `java-library`
}

group = "com.xcelerate"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.14.0")
}

kotlin {
    jvmToolchain(17)
}
