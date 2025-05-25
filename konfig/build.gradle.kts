import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(additionals.plugins.kotlin.multiplatform)
    alias(additionals.plugins.android.library)
    alias(additionals.plugins.multiplatform.buildkonfig)
    alias(additionals.plugins.kotlin.serialization)
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
            }
        }
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.get().compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
}

android {
    namespace = "io.vopenia.konfig"
}

buildkonfig {
    packageName = "io.vopenia.konfig"

    defaultConfigs {
        listOf(
            "tunnelEndpointTokenForwarder" to "VOPENIA_MEET_TESTS_TUNNEL_ENDPOINT",
            "tunnelApiForwarder" to "VOPENIA_MEET_TESTS_TUNNEL_API",
        ).forEach {
            buildConfigField(
                FieldSpec.Type.STRING,
                it.first,
                rootProject.extra[it.second] as String
            )
        }
    }
}
