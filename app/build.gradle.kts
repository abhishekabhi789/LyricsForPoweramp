plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    id("kotlin-parcelize")
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "io.github.abhishekabhi789.lyricsforpoweramp"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.abhishekabhi789.lyricsforpoweramp"
        minSdk = 21
        targetSdk = 35
        versionCode = 14
        versionName = "1.4"
        setProperty("archivesBaseName", "Lyrics4Poweramp-v$versionName")
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField(
            type = "String",
            name = "GITHUB_REPO_URL",
            value = "\"https://github.com/abhishekabhi789/LyricsForPoweramp\""
        )
        buildConfigField(
            type = "String",
            name = "PLAY_STORE_URL",
            value = "\"https://play.google.com/store/apps/details?id=io.github.abhishekabhi789.lyricsforpoweramp\""
        )
        buildConfigField(
            type = "String",
            name = "KO_FI_URL",
            value = "\"https://ko-fi.com/abhishekabhi789\""
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk { debugSymbolLevel = "FULL" }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":poweramp_api_lib"))
    implementation(libs.okhttp)
    implementation(libs.accompanist.permissions)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.gson)
    implementation(libs.core.splashscreen)
    implementation(libs.play.services.oss.licenses)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.foundation.android)
    implementation(libs.work.runtime.ktx)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(platform(libs.compose.bom))
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
