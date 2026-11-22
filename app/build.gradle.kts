plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.chaquo.python")
}

val youtubedlAndroid = "0.18.1"

android {
    namespace = "com.mediagrab.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mediagrab.app"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // Wajib untuk Chaquopy (sesuai dengan splits abi di bawah)
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    // Satu APK per arsitektur (lebih kecil) + satu APK universal.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            // Dimatikan: R8 bisa merusak library yt-dlp (refleksi + JSON).
            isMinifyEnabled = false
            // Ditandatangani dengan kunci debug supaya APK release bisa langsung dipasang.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    packaging {
        // Wajib untuk youtubedl-android: binary python/ffmpeg harus diekstrak ke disk.
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES")
        }
    }
}

// Konfigurasi Chaquopy untuk menginstal gallery-dl otomatis saat build
chaquopy {
    defaultConfig {
        version = "3.11"
        pip {
            install("gallery-dl")
        }
    }
}

dependencies {
    // Engine unduhan (yt-dlp + ffmpeg)
    implementation("io.github.junkfood02.youtubedl-android:library:$youtubedlAndroid")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlAndroid")

    // AndroidX + Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Pemutar (preview + putar hasil unduhan)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Gambar
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
