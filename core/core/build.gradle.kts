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

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {

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

    // Paging (for ILoginRecordRepository interface)
    api(libs.paging.runtime)

    implementation(libs.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.android.core.ktx)

    implementation(libs.androidx.fragment.ktx)
    // Navigation component
    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)

    implementation(libs.rxjava)
    implementation(libs.androidx.biometric)
    implementation(libs.material)

    implementation(project(":core:strings"))
    implementation(project(":core:ui-compose"))
    implementation(project(":core:navigation"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}