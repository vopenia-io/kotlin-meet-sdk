plugins {
    alias(additionals.plugins.kotlin.multiplatform)
    alias(additionals.plugins.android.library)
    id("org.jetbrains.kotlin.native.cocoapods")
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

    cocoapods {
        summary = "Meet SDK"
        homepage = "https://vopenia.io"
        version = "1.0"
        specRepos {
            url("https://github.com/livekit/podspecs")
        }
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "kotlin-meet-sdk"
            isStatic = true
        }

        pod("LiveKitClient") {
            version = "2.6.0"
            moduleName = "LiveKitClient"
            packageName = "LiveKitClient"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        pod("LiveKitClientKotlin") {
            version = "2.6.0"
            source = path(rootProject.file("../LiveKitClientKotlin"))
            moduleName = "LiveKitClientKotlin"
            packageName = "LiveKitClientKotlin"
            extraOpts += listOf("-compiler-option", "-fmodules")
            useInteropBindingFrom("LiveKitClient")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.kotlinMeetSdkApi)
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
