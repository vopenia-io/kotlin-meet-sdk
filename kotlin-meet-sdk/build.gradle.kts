plugins {
    alias(additionals.plugins.kotlin.multiplatform)
    alias(additionals.plugins.android.library)
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
                implementation(projects.kotlinMeetSdkApi)
                implementation(libs.vopenia)
                implementation(libs.vopenia.utils)
                api(libs.vopenia.participants)
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
