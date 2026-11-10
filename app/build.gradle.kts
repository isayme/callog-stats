import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.isayme.callstats"
    compileSdk = 36

    // CI passes -Pcallstats.versionName / -Pcallstats.versionCode derived from
    // the release tag; local builds fall back to the values below.
    val versionNameOverride = providers.gradleProperty("callstats.versionName").orNull
    val versionCodeOverride = providers.gradleProperty("callstats.versionCode").orNull?.toIntOrNull()

    defaultConfig {
        applicationId = "org.isayme.callstats"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeOverride ?: 1
        versionName = versionNameOverride ?: "0.1.0"
    }

    /*
     * The app is sideloaded, not distributed through a store, so release builds
     * share the debug key. The keystore is committed on purpose: CI and local
     * builds must produce identically signed APKs, otherwise a new release
     * cannot be installed over an existing install. The key is the standard
     * public Android debug key (alias/key password are well-known).
     */
    signingConfigs {
        getByName("debug") {
            storeFile = file("signing/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
