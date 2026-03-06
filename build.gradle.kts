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

        afterEvaluate {
            dependencies {
                "testImplementation"("org.testcontainers:testcontainers:1.20.4")
                "testImplementation"("org.testcontainers:postgresql:1.20.4")
                "testImplementation"("org.testcontainers:junit-jupiter:1.20.4")
            }

            apply(plugin = "jacoco")

            configure<JacocoPluginExtension> {
                toolVersion = "0.8.12"
            }

            tasks.withType<Test> {
                finalizedBy(tasks.named("jacocoTestReport"))
            }

            tasks.withType<JacocoReport> {
                dependsOn(tasks.withType<Test>())
                reports {
                    xml.required.set(true)
                    html.required.set(true)
                    csv.required.set(false)
                }
                classDirectories.setFrom(
                    files(
                        classDirectories.files.map {
                            fileTree(it) {
                                exclude(
                                    "**/grpc/**",
                                    "**/proto/**",
                                    "**/entity/**",
                                    "**/config/**",
                                    "**/resource/**",
                                    "openpos/**",
                                )
                            }
                        },
                    ),
                )
            }

            tasks.withType<JacocoCoverageVerification> {
                violationRules {
                    rule {
                        limit {
                            counter = "LINE"
                            minimum = "0.70".toBigDecimal()
                        }
                    }
                }
                classDirectories.setFrom(
                    files(
                        classDirectories.files.map {
                            fileTree(it) {
                                exclude(
                                    "**/grpc/**",
                                    "**/proto/**",
                                    "**/entity/**",
                                    "**/config/**",
                                    "**/resource/**",
                                    "openpos/**",
                                )
                            }
                        },
                    ),
                )
            }

            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(tasks.withType<JacocoCoverageVerification>())
            }
        }
    }
}
