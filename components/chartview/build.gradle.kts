plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    val minSdkVersion: Int = rootProject.ext.get("min_sdk_version") as Int
    val targetSdkVersion: Int = rootProject.ext.get("compile_sdk_version") as Int
    compileSdk = targetSdkVersion

    defaultConfig {
        minSdk = minSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
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
        viewBinding = true
        compose = true
    }
    namespace = "io.horizontalsystems.chartview"
}

dependencies {
//    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation(project(":core:core"))
    implementation(project(":core:ui-compose"))
    implementation(project(":core:strings"))
    implementation(project(":core:resources"))

    implementation(libs.rxjava)
    implementation(libs.kotlinx.coroutines.rx2)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
    implementation(libs.compose.tooling)
    implementation(libs.coil.compose)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.android.core.ktx)
}
