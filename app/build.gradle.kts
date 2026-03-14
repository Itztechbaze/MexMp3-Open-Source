plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.mexmp3.mextv"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mexmp3.mextv"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "2.0.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // ── Compose BOM (latest stable March 2026) ──────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ── Navigation ───────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // ── Lifecycle ────────────────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // ── Media3 ExoPlayer ─────────────────────────────────────────────────────
    implementation("androidx.media3:media3-exoplayer:1.6.0")
    implementation("androidx.media3:media3-session:1.6.0")
    implementation("androidx.media3:media3-ui:1.6.0")
    implementation("androidx.media3:media3-common:1.6.0")

    // ── Coil (image loading) ─────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── DataStore ────────────────────────────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.3")

    // ── Room ─────────────────────────────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // ── Firebase ─────────────────────────────────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // ── Core Android ─────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")

    // ── Media compat (MediaStyle notification, MediaSession) ─────────────────
    implementation("androidx.media:media:1.7.0")

    // ── Gson ─────────────────────────────────────────────────────────────────
    implementation("com.google.code.gson:gson:2.11.0")
}
