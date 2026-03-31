import org.gradle.api.tasks.Delete

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
            val deleteStaleRunnerJars =
                tasks.register<Delete>("deleteStaleRunnerJars") {
                    delete(
                        fileTree(layout.buildDirectory) {
                            include("${project.name}-*-runner.jar")
                            exclude("${project.name}-${project.version}-runner.jar")
                        },
                    )
                }

            dependencies {
                "implementation"(rootProject.libs.quarkus.logging.json)
                "testImplementation"(rootProject.libs.testcontainers)
                "testImplementation"(rootProject.libs.testcontainers.postgresql)
                "testImplementation"(rootProject.libs.testcontainers.junit.jupiter)
            }

            tasks.matching { it.name == "quarkusBuild" }.configureEach {
                dependsOn(deleteStaleRunnerJars)
            }

            apply(plugin = "jacoco")

            configure<JacocoPluginExtension> {
                toolVersion =
                    rootProject.libs.versions.jacoco
                        .get()
            }

            tasks.withType<Test> {
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
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
                                    "**/repository/**",
                                    "openpos/**",
                                    "**/ServiceHealthCheck*",
                                    "**/ServiceLivenessCheck*",
                                    "**/ServiceReadinessCheck*",
                                    "**/HealthResource*",
                                    "**/ProcessedEventEntity*",
                                    "**/ProcessedEventRepository*",
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
                            minimum = "0.95".toBigDecimal()
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
                                    "**/repository/**",
                                    "openpos/**",
                                    "**/ServiceHealthCheck*",
                                    "**/ServiceLivenessCheck*",
                                    "**/ServiceReadinessCheck*",
                                    "**/HealthResource*",
                                    "**/ProcessedEventEntity*",
                                    "**/ProcessedEventRepository*",
                                )
                            }
                        },
                    ),
                )
            }

            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(tasks.withType<JacocoCoverageVerification>())
            }

            // Mutation Testing (PIT) — local use only, not in CI
            // Gradle 9 is not supported by the pitest Gradle plugin, so we use a JavaExec task instead.
            val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
            val pitestVersion = catalog.findVersion("pitest").orElseThrow().requiredVersion
            val pitestJunit5Version = catalog.findVersion("pitest-junit5").orElseThrow().requiredVersion
            val pitestConf = configurations.create("pitest")
            dependencies {
                pitestConf("org.pitest:pitest-command-line:$pitestVersion")
                pitestConf("org.pitest:pitest-junit5-plugin:$pitestJunit5Version")
            }

            tasks.register<JavaExec>("pitest") {
                group = "verification"
                description = "Run PIT mutation testing (local use only)"
                dependsOn(tasks.named("testClasses"))
                mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
                classpath = pitestConf

                val mainOutput = project.layout.buildDirectory.dir("classes/kotlin/main")
                val testOutput = project.layout.buildDirectory.dir("classes/kotlin/test")
                val reportDir = project.layout.buildDirectory.dir("reports/pitest")
                val runtimeCp = configurations.getByName("testRuntimeClasspath")

                doFirst {
                    val cpEntries =
                        (runtimeCp.files + mainOutput.get().asFile + testOutput.get().asFile)
                            .joinToString(File.pathSeparator) { it.absolutePath }

                    args =
                        listOf(
                            "--targetClasses",
                            "com.openpos.*",
                            "--targetTests",
                            "com.openpos.*",
                            "--sourceDirs",
                            "src/main/kotlin",
                            "--reportDir",
                            reportDir.get().asFile.absolutePath,
                            "--classPath",
                            cpEntries,
                            "--threads",
                            Runtime.getRuntime().availableProcessors().toString(),
                            "--outputFormats",
                            "HTML,XML",
                            "--timestampedReports=false",
                            "--mutators",
                            "DEFAULTS",
                        )
                }
            }
        }
    }
}
