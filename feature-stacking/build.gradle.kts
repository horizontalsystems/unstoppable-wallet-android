plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "cash.p.terminal.featureStacking"

    compileSdk = 34
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    defaultConfig {
        minSdk = 27
        buildConfigField("String", "PIRATE_CONTRACT", "\"0xaFCC12e4040615E7Afe9fb4330eB3D9120acAC05\"")
        buildConfigField("String", "COSANTA_CONTRACT", "\"0x5F980533B994c93631A639dEdA7892fC49995839\"")
        buildFeatures {
            compose = true
        }
    }
}

dependencies {
    implementation(project(":core:ui-compose"))
    implementation(project(":core:strings"))
    implementation(project(":core:core"))
    implementation(project(":core:resources"))
    implementation(project(":core:wallet"))
    implementation(project(":core:network"))
    implementation(project(":core:navigation"))
    implementation(project(":components:chartview"))

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
    implementation(libs.compose.tooling)
    implementation(libs.javax.inject)
    implementation(libs.rxjava)
    implementation(libs.kotlinx.coroutines.rx2)

    implementation(libs.androidx.navigation.runtime.ktx)
    debugImplementation(libs.androidx.ui.tooling)
}
