import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    // Only for the resource loader: the content documents are this service's database, and
    // Compose Resources is the one API that reads bundled files on both Android and iOS
    // without an expect/actual of its own. Reading a bundled file needs a Context on
    // Android and an NSBundle on iOS, and hand-writing that expect/actual is exactly the
    // work this library already did.
    //
    // The price is a Compose dependency in a module with no UI, which is not free — but
    // the alternatives are worse: a platform source set per target, or baking 45 kB of
    // JSON into string constants and hitting the JVM's 64 kB limit on the next lesson.
    alias(libs.plugins.composeMultiplatform)
    // Required by the Compose plugin since 1.6.10, even for a resource-only module.
    alias(libs.plugins.composeCompiler)
}

compose.resources {
    // A library module, so the accessors must be generated unconditionally, and under this
    // module's own package to avoid clashing with the app's Res.
    generateResClass = always
    packageOfResClass = "pl.prodevcode.learnmobiledev.fakeapi.resources"
}

kotlin {
    // No framework here: the module is consumed by :shared and is linked into its
    // framework. A second framework would ship the same code twice.
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "pl.prodevcode.learnmobiledev.fakeapi"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        // Without this the content documents never reach the APK: Compose Resources has no
        // asset directory to copy them into, and every request 404s at runtime.
        androidResources {
            enable = true
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.mock)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
