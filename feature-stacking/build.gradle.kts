plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)

    id(libs.plugins.kotlin.parcelize.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "cash.p.terminal.featureStacking"

    compileSdk = 36
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        minSdk = 27
        vectorDrawables.generatedDensities?.clear()
        buildFeatures {
            compose = true
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
    implementation(project(":feature-premium"))

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.material3.android)
    implementation(libs.compose.tooling)
    implementation(libs.javax.inject)
    implementation(libs.rxjava)
    implementation(libs.kotlinx.coroutines.rx2)
    implementation(libs.timber)

    implementation(libs.androidx.navigation.runtime.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)
}
