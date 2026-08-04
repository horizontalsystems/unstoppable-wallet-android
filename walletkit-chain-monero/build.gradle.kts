plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "io.horizontalsystems.walletkit.chain.monero"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":walletkit"))

    api(libs.kit.monero)

    testImplementation(libs.junit)
}

// Forward -PupdateParityFixture=true to the test JVM so ChainBehaviorParityTest can
// regenerate its golden fixture (see docs/Walletkit-Modularization-Plan.md).
tasks.withType<Test>().configureEach {
    systemProperty(
        "updateParityFixture",
        providers.gradleProperty("updateParityFixture").getOrElse("false")
    )
}
