plugins {
    kotlin("jvm") apply false
    kotlin("plugin.allopen") apply false
    kotlin("plugin.noarg") apply false
    id("io.quarkus") apply false
}

allprojects {
    group = "com.openpos"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
