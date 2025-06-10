plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "cash.p.terminal.tangem"

    compileSdk = 34
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        minSdk = 27
        buildFeatures {
            compose = true
        }
    }
    buildFeatures {
        buildConfig = true
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

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.material3.android)
    implementation(libs.compose.tooling)
    implementation(libs.javax.inject)

    implementation(libs.tangem.sdk)
    implementation(libs.bitcoin.kit)

    implementation(libs.ethereum.kit)
    implementation(libs.solanakt)
    implementation(libs.ton.kotlin.contract)
    implementation(libs.ton.kit)
    implementation(libs.tron.kit)
    implementation(libs.binance.kit)

    implementation(libs.androidx.navigation.runtime.ktx)
    debugImplementation(libs.androidx.ui.tooling)
}
