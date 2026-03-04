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

subprojects {
    if (project.path.startsWith(":services:")) {
        val copyProto =
            tasks.register<Sync>("copyProto") {
                from(rootProject.file("proto/openpos"))
                into(project.file("src/main/proto/openpos"))
            }

        tasks.matching { it.name == "quarkusGenerateCode" || it.name == "quarkusGenerateCodeDev" }.configureEach {
            dependsOn(copyProto)
        }
    }
}
