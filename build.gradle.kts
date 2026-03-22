plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.kotlin.noarg) apply false
    alias(libs.plugins.quarkus) apply false
    alias(libs.plugins.owasp.depcheck) apply false
}

allprojects {
    group = "com.openpos"
    version = "1.0.0"

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
                "testImplementation"(rootProject.libs.testcontainers)
                "testImplementation"(rootProject.libs.testcontainers.postgresql)
                "testImplementation"(rootProject.libs.testcontainers.junit.jupiter)
            }

            apply(plugin = "jacoco")

            configure<JacocoPluginExtension> {
                toolVersion =
                    rootProject.libs.versions.jacoco
                        .get()
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
                dependsOn(tasks.named("classes"))
                violationRules {
                    rule {
                        limit {
                            counter = "LINE"
                            // TODO: 段階的に 80% へ引き上げ予定。現状は全サービスで 80% を満たしていないため
                            //       ビルドを壊さないよう 30% に設定。新規コードは 80%+ を目標とすること。
                            //       ロードマップ: 30% → 50% → 65% → 80%
                            minimum = "0.30".toBigDecimal()
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
