import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    application
    alias(additionals.plugins.kotlin.jvm)
    alias(additionals.plugins.kotlin.serialization)
    id("jvmCompat")
}

val mainClassInManifest = "io.vopenia.ApplicationKt"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

application {
    mainClass.set(mainClassInManifest)

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest {
        attributes["Main-Class"] = mainClassInManifest
    }
}

dependencies {
    api(project(":konfig"))
    api(additionals.kotlinx.coroutines)
    api(additionals.kotlinx.coroutines.jvm)
    api(libs.ktor.server.core)
    api(libs.ktor.server.netty)
    api(libs.ktor.server.contentnegociation)
    api(libs.ktor.server.compression)
    api(libs.ktor.server.json)
    api(libs.ktor.server.cors)
    api(libs.ktor.client.content.negotiation)
}
