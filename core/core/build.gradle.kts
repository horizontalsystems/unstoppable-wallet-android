plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    alias(libs.plugins.compose.compiler)
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
    val kotlin_version = rootProject.ext.get("kotlin_version") as String
    val appcompat_version = rootProject.ext.get("appcompat_version") as String
    val constraint_version = rootProject.ext.get("constraint_version") as String
    val fragment_ktx_version = rootProject.ext.get("fragment_ktx_version") as String
    val navigation_ktx_version = rootProject.ext.get("navigation_ktx_version") as String
    val rxjava_version = rootProject.ext.get("rxjava_version") as String
    val biometric_version = rootProject.ext.get("biometric_version") as String
    val material_version = rootProject.ext.get("material_version") as String
    val junit_version = rootProject.ext.get("junit_version") as String

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version")
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("androidx.constraintlayout:constraintlayout:$constraint_version")
    implementation(libs.android.core.ktx)

    implementation("androidx.fragment:fragment-ktx:$fragment_ktx_version")
    // Navigation component
    api("androidx.navigation:navigation-fragment-ktx:$navigation_ktx_version")
    api("androidx.navigation:navigation-ui-ktx:$navigation_ktx_version")

    implementation("io.reactivex.rxjava2:rxjava:$rxjava_version")
    implementation("androidx.biometric:biometric:$biometric_version")
    implementation("com.google.android.material:material:$material_version")

    implementation(project(":core:strings"))
    implementation(project(":core:ui-compose"))

    testImplementation("junit:junit:$junit_version")
}