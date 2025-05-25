plugins {
    alias(additionals.plugins.kotlin.multiplatform)
    alias(additionals.plugins.android.library)
    alias(additionals.plugins.kotlin.serialization)
    id("publication")
    id("jvmCompat")
    id("iosSimulatorConfiguration")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    jvm()

    js(IR) {
        browser()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosArm64()
    macosX64()

    mingwX64()

    linuxArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(additionals.multiplatform.http.client)
                implementation(additionals.kotlinx.coroutines)
                implementation(additionals.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(additionals.multiplatform.file.access)
                implementation(additionals.kotlinx.coroutines.test)
                implementation(projects.konfig)
            }
        }
    }
}

android {
    namespace = rootProject.getExtraString("group", "")
}
