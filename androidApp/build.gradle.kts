import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    // Ktor's JVM internals bind to SLF4J, and Android ships no provider — SLF4J then
    // prints three "No SLF4J providers were found" warnings to System.err on first use.
    // The app's HTTP logging goes through Ktor's own Logger, so nothing is lost by
    // binding the API to a no-op implementation; it only silences the startup noise.
    runtimeOnly(libs.slf4j.nop)
}

android {
    namespace = "pl.prodevcode.learnmobiledev"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pl.prodevcode.learnmobiledev"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Read from gradle.properties so the version lives in one place and CI can
        // override it with -Papp.versionCode=<run number> without touching the build file.
        versionCode = (project.findProperty("app.versionCode") as String).toInt()
        versionName = project.findProperty("app.versionName") as String
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            // Signed with the debug key on purpose: this is a teaching project with no
            // release keystore, and an unsigned APK cannot be installed at all. Replace
            // this with a real signingConfig before shipping anywhere.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}