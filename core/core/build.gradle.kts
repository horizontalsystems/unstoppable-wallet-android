plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    alias(libs.plugins.compose.compiler)
    id(libs.plugins.devtools.ksp.get().pluginId)
}

android {
    val minSdkVersion: Int = rootProject.ext.get("min_sdk_version") as Int
    val targetSdkVersion: Int = rootProject.ext.get("compile_sdk_version") as Int

    compileSdk = targetSdkVersion
    namespace = "cash.p.terminal.core"

    defaultConfig {
        minSdk = minSdkVersion

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
}

dependencies {
    val appcompat_version = rootProject.ext.get("appcompat_version") as String
    val constraint_version = rootProject.ext.get("constraint_version") as String
    val rxjava_version = rootProject.ext.get("rxjava_version") as String
    val biometric_version = rootProject.ext.get("biometric_version") as String
    val junit_version = rootProject.ext.get("junit_version") as String

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.material3.android)
    ksp(libs.room.compiler)

    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("androidx.constraintlayout:constraintlayout:$constraint_version")
    implementation(libs.android.core.ktx)

    implementation(libs.androidx.fragment.ktx)
    // Navigation component
    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)

    implementation("io.reactivex.rxjava2:rxjava:$rxjava_version")
    implementation("androidx.biometric:biometric:$biometric_version")
    implementation(libs.material)

    implementation(project(":core:strings"))
    implementation(project(":core:ui-compose"))
    implementation(project(":core:navigation"))

    testImplementation("junit:junit:$junit_version")
}