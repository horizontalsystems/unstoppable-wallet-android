plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "cash.p.terminal.ui_compose"
    compileSdk = 34

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    val minSdkVersion: Int = rootProject.ext.get("min_sdk_version") as Int
    defaultConfig {
        minSdk = minSdkVersion
        buildFeatures {
            compose = true
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
    implementation(libs.compose.tooling)
    implementation(libs.coil.compose)

    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)

    implementation(libs.androidx.material3.android)
    implementation(libs.material)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(project(":core:strings"))
    implementation(project(":core:resources"))
}