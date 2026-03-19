pluginManagement {
    val quarkusPluginVersion: String by settings
    val kotlinVersion = "2.3.20"
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusPluginVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.allopen") version kotlinVersion
        kotlin("plugin.noarg") version kotlinVersion
    }
}

rootProject.name = "open-pos"

include(
    "services:api-gateway",
    "services:pos-service",
    "services:product-service",
    "services:inventory-service",
    "services:analytics-service",
    "services:store-service",
)
