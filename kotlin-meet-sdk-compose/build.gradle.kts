plugins {
    alias(additionals.plugins.kotlin.multiplatform)
    alias(additionals.plugins.android.library)
    alias(additionals.plugins.jetbrains.compose)
    alias(additionals.plugins.compose.compiler)
    // alias(additionals.plugins.kotlin.cocoapods)
    id("jvmCompat")
    id("iosSimulatorConfiguration")
    id("publication")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    /*
    cocoapods {
        summary = "UI Compose library for meet"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        osx.deploymentTarget = "16.0"
        framework {
            baseName = "meet-compose"
            isStatic = false
            transitiveExport = true
        }

        pod("LiveKitClient") {
            version = "2.6.0"
            source = path(rootProject.file("../LiveKitClient"))
            moduleName = "LiveKitClient"
            packageName = "LiveKitClient"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }
     */

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)

            implementation(projects.kotlinMeetSdk)
            implementation(libs.vopenia)
            implementation(libs.vopenia.compose)
        }
    }
}

android {
    namespace = "${rootProject.getExtraString("group", "")}.compose"
}
