pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
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
