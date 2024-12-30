plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "cash.p.terminal.strings"
    compileSdk = 34

    val minSdkVersion: Int = rootProject.ext.get("min_sdk_version") as Int
    defaultConfig {
        minSdk = minSdkVersion
        buildFeatures {
            compose = true
        }
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
}