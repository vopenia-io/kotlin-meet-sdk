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

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinMeetSdkApi)
                api(libs.vopenia)
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
