rootProject.name = "open-pos"

include(
    "services:api-gateway",
    "services:pos-service",
    "services:product-service",
    "services:inventory-service",
    "services:analytics-service",
    "services:store-service",
)

pluginManagement {
    val quarkusPluginVersion: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusPluginVersion
    }
}
