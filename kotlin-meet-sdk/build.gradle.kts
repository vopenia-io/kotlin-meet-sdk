plugins {
    alias(additionals.plugins.kotlin.multiplatform)
    alias(additionals.plugins.android.library)
    alias(additionals.plugins.kotlin.serialization)
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

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.PodInstallSyntheticTask>()
    .configureEach {
        doLast {
            val xcodeprojFiles = listOf(
                "Pods/Pods.xcodeproj",
                "synthetic.xcodeproj",
            )

            for (xcodeprojFile in xcodeprojFiles) {
                val file =
                    project.buildDir.resolve("cocoapods/synthetic/ios/$xcodeprojFile/project.pbxproj")
                setIosDeploymentTarget(file)
            }
        }
    }

// Every pod of the synthetic project builds for 16.0, the apps' minimum. Pods otherwise
// keep their podspec target (12.0, 13.0) and Xcode 27 refuses anything below 15.0.
fun setIosDeploymentTarget(
    xcodeprojFile: File,
    target: String = "16.0",
) {
    if (!xcodeprojFile.exists()) {
        return
    }

    val lines = xcodeprojFile.readLines()
    val out = xcodeprojFile.bufferedWriter()
    out.use {
        for (line in lines) {
            out.write(
                line.replace(
                    "IPHONEOS_DEPLOYMENT_TARGET = ",
                    "IPHONEOS_DEPLOYMENT_TARGET = $target; // "
                )
            )
            out.write(("\n"))
        }
    }
}
