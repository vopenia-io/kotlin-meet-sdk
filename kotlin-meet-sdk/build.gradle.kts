plugins {
    alias(additionals.plugins.kotlin.multiplatform)
    alias(additionals.plugins.android.library)
    alias(additionals.plugins.kotlin.serialization)
    id("publication")
    id("jvmCompat")
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
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = rootProject.getExtraString("group", "")
}
