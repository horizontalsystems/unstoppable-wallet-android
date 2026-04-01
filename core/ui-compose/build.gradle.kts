plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)

    id(libs.plugins.kotlin.parcelize.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "cash.p.terminal.ui_compose"
    compileSdk = 36

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        vectorDrawables.generatedDensities?.clear()
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
    val minSdkVersion: Int = rootProject.ext.get("min_sdk_version") as Int
    defaultConfig {
        minSdk = minSdkVersion
        buildFeatures {
            compose = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

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

    implementation(project(":components:icons"))
    implementation(project(":core:strings"))
    implementation(project(":core:resources"))
    implementation(project(":core:navigation"))
}