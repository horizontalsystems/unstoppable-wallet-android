import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id(libs.plugins.devtools.ksp.get().pluginId)
}

kotlin {
    androidTarget()

    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11" // Use the same JVM target as Java
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.kotlinx.serialization)
                implementation(libs.ktor.client.log)

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.ktor.kotlinx.serialization)

                implementation(libs.room.runtime)
                implementation(libs.room.ktx)
            }
        }
        androidMain {
            dependencies {
            }
        }
        iosMain {
        }
        commonTest {
        }
    }
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    val minSdkVersion: Int = rootProject.ext.get("min_sdk_version") as Int
    val targetSdkVersion: Int = rootProject.ext.get("compile_sdk_version") as Int

    namespace = "cash.p.terminal.network"
    compileSdk = targetSdkVersion
    defaultConfig {
        minSdk = minSdkVersion
    }
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
}