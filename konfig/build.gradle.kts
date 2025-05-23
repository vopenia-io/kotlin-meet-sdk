import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(additionals.plugins.kotlin.multiplatform)
    alias(additionals.plugins.android.library)
    alias(additionals.plugins.multiplatform.buildkonfig)
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
            "cookieToken" to "VOPENIA_MEET_TESTS_COOKIE_TOKEN_VALUE"
        ).forEach {
            buildConfigField(
                FieldSpec.Type.STRING,
                it.first,
                rootProject.extra[it.second] as String
            )
        }
    }
}
