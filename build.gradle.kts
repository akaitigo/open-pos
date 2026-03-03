plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.allopen") version "2.1.10" apply false
    kotlin("plugin.noarg") version "2.1.10" apply false
    id("io.quarkus") version "3.17.8" apply false
}

allprojects {
    group = "com.openpos"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
