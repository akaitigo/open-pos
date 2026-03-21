plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.noarg)
    alias(libs.plugins.quarkus)
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.grpc)
    implementation(libs.quarkus.hibernate.orm.panache.kotlin)
    implementation(libs.quarkus.jdbc.postgresql)
    implementation(libs.quarkus.flyway)
    implementation(libs.quarkus.redis.client)
    implementation(libs.quarkus.messaging.rabbitmq)
    implementation(libs.quarkus.smallrye.health)
    implementation(libs.quarkus.micrometer.registry.prometheus)
    implementation(libs.quarkus.opentelemetry)
    implementation(libs.kotlin.stdlib)
    implementation(libs.quarkus.scheduler)
    implementation(libs.quarkus.arc)

    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.quarkus.junit5.mockito)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.quarkus.jdbc.h2)
    testImplementation(libs.rest.assured)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        javaParameters.set(true)
    }
}
