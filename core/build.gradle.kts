import java.util.concurrent.TimeUnit

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "io.horizontalsystems.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.bouncycastle:bcprov-jdk15to18:1.68")).using(module("org.bouncycastle:bcprov-jdk15on:1.70"))
            substitute(module("com.google.protobuf:protobuf-java:3.6.1")).using(module("com.google.protobuf:protobuf-javalite:3.21.1"))
            substitute(module("net.jcip:jcip-annotations:1.0")).using(module("com.github.stephenc.jcip:jcip-annotations:1.0-1"))

            substitute(module("com.tinder.scarlet:scarlet:0.1.12")).using(module("com.walletconnect.Scarlet:scarlet:1.0.2"))
            substitute(module("com.tinder.scarlet:websocket-okhttp:0.1.12")).using(module("com.walletconnect.Scarlet:websocket-okhttp:1.0.2"))
            substitute(module("com.tinder.scarlet:stream-adapter-rxjava2:0.1.12")).using(module("com.walletconnect.Scarlet:stream-adapter-rxjava2:1.0.2"))
            substitute(module("com.tinder.scarlet:message-adapter-gson:0.1.12")).using(module("com.walletconnect.Scarlet:message-adapter-gson:1.0.2"))
            substitute(module("com.tinder.scarlet:lifecycle-android:0.1.12")).using(module("com.walletconnect.Scarlet:lifecycle-android:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:scarlet:1.0.0")).using(module("com.walletconnect.Scarlet:scarlet:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:websocket-okhttp:1.0.0")).using(module("com.walletconnect.Scarlet:websocket-okhttp:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:stream-adapter-rxjava2:1.0.0")).using(module("com.walletconnect.Scarlet:stream-adapter-rxjava2:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:message-adapter-gson:1.0.0")).using(module("com.walletconnect.Scarlet:message-adapter-gson:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:lifecycle-android:1.0.0")).using(module("com.walletconnect.Scarlet:lifecycle-android:1.0.2"))
        }

        resolutionStrategy.eachDependency {
            if (requested.group == "com.squareup.okhttp3") {
                useVersion("4.12.0")
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // AndroidX Core
    api(libs.androidx.appcompat)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference.ktx)
    api(libs.androidx.core.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.browser)
    api(libs.androidx.biometric)
    implementation(libs.androidx.credentials)

    // Lifecycle
    implementation(libs.androidx.lifecycle.extensions)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.reactivestreams.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Navigation 3
    api(libs.androidx.navigation3.ui)
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.lifecycle.viewmodel.navigation3)
    api(libs.androidx.material3.adaptive.navigation3)
    api(libs.kotlinx.serialization.core)

    // Room
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    api(libs.androidx.room.rxjava2)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    api(libs.androidx.work.runtime.ktx)
    api(libs.androidx.work.rxjava2)

    // Compose
    api(libs.androidx.activity.compose)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.material)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.runtime.livedata)
    api(libs.androidx.compose.material3)

    // Google Material
    api(libs.google.material)

    // Accompanist
    api(libs.accompanist.navigation.animation)
    api(libs.accompanist.appcompat.theme)
    api(libs.accompanist.flowlayout)
    api(libs.accompanist.permissions)

    // Networking
    api(libs.retrofit)
    api(libs.retrofit.adapter.rxjava2)
    api(libs.retrofit.converter.gson)
    api(libs.retrofit.converter.scalars)
    api(libs.okhttp.logging)
    api(libs.gson)

    // Rx
    api(libs.rxjava)
    api(libs.rxandroid)

    // Coroutines
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.rx2)

    // Image loading
    api(libs.coil.compose)
    api(libs.coil.svg)
    api(libs.coil.gif)

    // Logging
    api(libs.timber)

    // Markdown
    api(libs.commonmark)
    api(libs.markwon)

    // QR
    api(libs.zxing)
    api(libs.qrose)

    // Web3
    api(libs.web3j)
    api(libs.unstoppable.domains)

    // Wallet Kits
    api(libs.kit.monero)
    api(libs.kit.zano)
    api(libs.kit.stellar)
    api(libs.kit.ton)
    api(libs.kit.bitcoin)
    api(libs.kit.ethereum)
    api(libs.kit.fee.rate)
    api(libs.kit.market)
    api(libs.kit.solana)
    api(libs.kit.tron)
    api(libs.zcash.android.sdk)

    // BouncyCastle
    api(libs.bouncycastle)

    // Binance
    api(libs.binance.connector) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }

    // Desugar
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Exclude old version from wherever it's coming
    configurations.configureEach {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
        exclude(group = "org.bouncycastle", module = "bcutil-jdk18on")
    }

    // Tor
    api(libs.tor.android)
    api(libs.jtorctl)

    // Utils
    api(libs.circleindicator)
    api(libs.twitter.text)
    api(libs.android.shell)
    api(libs.portmapper)

    // Project modules
    api(project(":components:icons"))
    api(project(":components:chartview"))
    api(project(":subscriptions-core"))
    api(project(":dapp-core"))

    testImplementation(libs.junit)
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, TimeUnit.SECONDS)
        force("junit:junit:4.13.2")

        // Force Ktor version for TonKit
        force("io.ktor:ktor-utils:2.3.7")
        force("io.ktor:ktor-io:2.3.7")
        force("io.ktor:ktor-client-core:2.3.7")
    }
}
