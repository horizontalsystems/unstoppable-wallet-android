plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
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
    namespace = "io.horizontalsystems.bankwallet"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.horizontalsystems.bankwallet"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 161
        versionName = "0.48.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations += listOf("de", "es", "en", "fa", "fr", "ko", "pt", "pt-rBR", "ru", "tr", "zh")

        vectorDrawables.useSupportLibrary = true

        resValue("string", "companyWebPageLink", "https://horizontalsystems.io")
        resValue("string", "appWebPageLink", "https://unstoppable.money")
        resValue("string", "analyticsLink", "https://unstoppable.money/analytics")
        resValue("string", "appGithubLink", "https://github.com/horizontalsystems/unstoppable-wallet-android")
        resValue("string", "appTwitterLink", "https://twitter.com/UnstoppableByHS")
        resValue("string", "appTelegramLink", "https://t.me/unstoppable_announcements")
        resValue("string", "reportEmail", "support.unstoppable@protonmail.com")
        resValue("string", "releaseNotesUrl", "https://api.github.com/repos/horizontalsystems/unstoppable-wallet-android/releases/tags/")
        resValue("string", "walletConnectAppMetaDataName", "Unstoppable")
        resValue("string", "walletConnectAppMetaDataUrl", "unstoppable.money")
        resValue("string", "walletConnectAppMetaDataIcon", "https://raw.githubusercontent.com/horizontalsystems/HS-Design/master/PressKit/UW-AppIcon-on-light.png")
        resValue("string", "accountsBackupFileSalt", "unstoppable")

        buildConfigField("boolean", "FDROID_BUILD", "false")

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    signingConfigs {
        create("appCenter") {
            storeFile = file("./test.keystore")
            storePassword = "testKeystore123"
            keyAlias = "testKeystore"
            keyPassword = "testKeystore123"
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("base") {
            dimension = "distribution"
            signingConfig = signingConfigs.getByName("debug")
        }

        create("fdroid") {
            dimension = "distribution"
            buildConfigField("boolean", "FDROID_BUILD", "true")
        }

        create("fdroidCi") {
            dimension = "distribution"
            applicationIdSuffix = ".fdroidci"
            buildConfigField("boolean", "FDROID_BUILD", "true")
            signingConfig = signingConfigs.getByName("appCenter")
        }

        create("ci") {
            dimension = "distribution"
            applicationIdSuffix = ".appcenter"
            versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: defaultConfig.versionCode
            signingConfig = signingConfigs.getByName("appCenter")
        }
    }

    buildTypes {
        debug {
            signingConfig = null
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".dev"
            resValue("string", "twitterBearerToken", "AAAAAAAAAAAAAAAAAAAAAJgeNwEAAAAA6xVpR6xLKTrxIA3kkSyRA92LDpA%3Da6auybDwcymUyh2BcS6zZwicUdxGtrzJC0qvOSdRwKLeqBGhwB")
            resValue("string", "etherscanKey", "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE")
            resValue("string", "bscscanKey", "R396MSJNCKX2YK4EIMP3EWYAW21NSVMXRN")
            resValue("string", "otherScanKey", "FU7CYEXQEUSMXJJF8MZR6BNRMP9XT8S9CP")
            resValue("string", "is_release", "false")
            resValue("string", "guidesUrl", "https://raw.githubusercontent.com/horizontalsystems/blockchain-crypto-guides/v1.2/index.json")
            resValue("string", "eduUrl", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/refs/tags/v1.4/src/edu.json")
            resValue("string", "faqUrl", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/master/src/faq.json")
            resValue("string", "coinsJsonUrl", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/master/coins.json")
            resValue("string", "providerCoinsJsonUrl", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/master/provider.coins.json")
            resValue("string", "marketApiBaseUrl", "https://api-dev.blocksdecoded.com")
            resValue("string", "marketApiKey", "IQf1uAjkthZp1i2pYzkXFDom")
            resValue("string", "openSeaApiKey", "bfbd6061a33e455c8581b594774fecb3")
            resValue("string", "walletConnectV2Key", "8b4f41c60880a3e3ad57d82fddb30568")
            resValue("string", "solanaAlchemyApiKey", "PKgWxOMarrHgyMESGjIkJ")
            resValue("string", "solanaJupiterApiKey", "ec901a97-0375-45b1-8b7d-da1ea9934cb0")
            resValue("string", "trongridApiKeys", "33374494-8060-447e-8367-90c5efd4ed95")
            resValue("string", "udnApiKey", "r2phzgatt_zt9-hd_wyvdjrdsrimnxgokm7knyag1malzgcz")
            resValue("string", "oneInchApiKey", "3EttyCzgWb2GLFIRoPIUYM0M4uKAVEcq")
            resValue("string", "blocksDecodedEthereumRpc", "https://api-dev.blocksdecoded.com/v1/ethereum-rpc/mainnet")
            resValue("string", "chainalysisBaseUrl", "https://public.chainalysis.com/api/v1/")
            resValue("string", "chainalysisApiKey", "928bb256db73f1cb93e1b3366a145d9fbe06e28581c8b665b82ad70bbfef1db4")
            resValue("string", "hashDitBaseUrl", "https://api.diting.pro/v2/hashdit/")
            resValue("string", "hashDitApiKey", "KuyxZfvJXFrpAcztshhYqeWaRusxyGRDDhFYkeIw")
            resValue("string", "uswapApiBaseUrl", "https://swap-dev.unstoppable.money/api/v1/")
            resValue("string", "uswapApiKey", "44fc76602e17e0c8259b6ce3bae3ca90804c6fd8f42ca00e6943a6b1ba7fe242")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "twitterBearerToken", "AAAAAAAAAAAAAAAAAAAAAJgeNwEAAAAA6xVpR6xLKTrxIA3kkSyRA92LDpA%3Da6auybDwcymUyh2BcS6zZwicUdxGtrzJC0qvOSdRwKLeqBGhwB")
            resValue("string", "etherscanKey", "IEXTB9RE7MUV2UQ9X238RP146IEJB1J5HS,27S4V3GYJGMCPWQZ2T4SF9355QBQYQ3FI7,YK4KEA3TANM8KZ5J6E2Q1ZIM6YDM8TEABM")
            resValue("string", "bscscanKey", "FQ2HSNNEHVG71U96P1TF3WF9RTF6AF5MRA,G6K8VZDWYSJHTCRURRITFZ2ZWV48GRGTZQ,R396MSJNCKX2YK4EIMP3EWYAW21NSVMXRN,")
            resValue("string", "otherScanKey", "Y855XHV4XKUC9DTRM2ZQG8XAQ96EJV221Q,43DEJEEMA1P81YAU555A1TECRY5FPIWCFH")
            resValue("string", "is_release", "true")
            resValue("string", "guidesUrl", "https://raw.githubusercontent.com/horizontalsystems/blockchain-crypto-guides/v1.2/index.json")
            resValue("string", "eduUrl", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/refs/tags/v1.4/src/edu.json")
            resValue("string", "faqUrl", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/v1.3/src/faq.json")
            resValue("string", "coinsJsonUrl", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/v0.21/coins.json")
            resValue("string", "providerCoinsJsonUrl", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/v0.21/provider.coins.json")
            resValue("string", "marketApiBaseUrl", "https://api.blocksdecoded.com")
            resValue("string", "marketApiKey", "IQf1uAjkthZp1i2pYzkXFDom")
            resValue("string", "openSeaApiKey", "bfbd6061a33e455c8581b594774fecb3")
            resValue("string", "walletConnectV2Key", "0c5ca155c2f165a7d0c88686f2113a72")
            resValue("string", "solanaAlchemyApiKey", "PKgWxOMarrHgyMESGjIkJ")
            resValue("string", "solanaJupiterApiKey", "ec901a97-0375-45b1-8b7d-da1ea9934cb0")
            resValue("string", "trongridApiKeys", "8f5ae2c8-8012-42a8-b0ca-ffc2741f6a29,578aa64f-a79f-4ee8-86e9-e9860e2d050a,1e92f1fc-41f8-401f-a7f6-5b719b6f1280")
            resValue("string", "udnApiKey", "r2phzgatt_zt9-hd_wyvdjrdsrimnxgokm7knyag1malzgcz")
            resValue("string", "oneInchApiKey", "3EttyCzgWb2GLFIRoPIUYM0M4uKAVEcq")
            resValue("string", "blocksDecodedEthereumRpc", "https://api.blocksdecoded.com/v1/ethereum-rpc/mainnet")
            resValue("string", "chainalysisBaseUrl", "https://public.chainalysis.com/api/v1/")
            resValue("string", "chainalysisApiKey", "928bb256db73f1cb93e1b3366a145d9fbe06e28581c8b665b82ad70bbfef1db4")
            resValue("string", "hashDitBaseUrl", "https://service.hashdit.io/v2/hashdit/")
            resValue("string", "hashDitApiKey", "aGMkgODYiUFtTYrSRcEZsIfPHeASOlGYXClJZNWF")
            resValue("string", "uswapApiBaseUrl", "https://swap-api.unstoppable.money/v1/")
            resValue("string", "uswapApiKey", "44fc76602e17e0c8259b6ce3bae3ca90804c6fd8f42ca00e6943a6b1ba7fe242")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            pickFirsts += listOf(
                    "META-INF/atomicfu.kotlin_module",
                    "META-INF/FastDoubleParser-LICENSE",
                    "META-INF/FastDoubleParser-NOTICE",
                    "META-INF/io.netty.versions.properties"
            )
            excludes += listOf(
                    "META-INF/INDEX.LIST",
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE.md"
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    lint {
        disable += "LogNotTimber"
        disable += "RemoveWorkManagerInitializer"
    }

    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.bouncycastle:bcprov-jdk15to18:1.68")).using(module("org.bouncycastle:bcprov-jdk15on:1.65"))
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.biometric)

    // Lifecycle
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.reactivestreams.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.rxjava2)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.rxjava2)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.material3)

    // Google Material
    implementation(libs.google.material)

    // Accompanist
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.appcompat.theme)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.permissions)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.adapter.rxjava2)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Rx
    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx2)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)

    // Logging
    implementation(libs.timber)
    debugImplementation(libs.leakcanary)

    // Markdown
    implementation(libs.commonmark)
    implementation(libs.markwon)

    // QR
    api(libs.zxing)
    implementation(libs.qrose)

    // Reown (WalletConnect)
    implementation(platform(libs.reown.bom))
    implementation(libs.reown.walletkit) {
        exclude(group = "com.google.firebase")
    }
    implementation(libs.reown.android.core) {
        exclude(group = "com.google.firebase")
    }

    // Web3
    implementation(libs.web3j)
    implementation(libs.unstoppable.domains)

    // Wallet Kits
    implementation(libs.kit.monero)
    implementation(libs.kit.stellar)
    implementation(libs.kit.ton)
    implementation(libs.kit.bitcoin)
    implementation(libs.kit.ethereum)
    implementation(libs.kit.fee.rate)
    implementation(libs.kit.market)
    implementation(libs.kit.solana)
    implementation(libs.kit.tron)
    implementation(libs.kit.zcash)

    // Binance
    implementation(libs.binance.connector) {
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
    implementation(libs.tor.android)
    implementation(libs.jtorctl)

    // Utils
    implementation(libs.circleindicator)
    implementation(libs.twitter.text)
    api(libs.android.shell)
    api(libs.portmapper)

    // UI modules
    implementation(project(":core"))
    implementation(project(":components:icons"))
    implementation(project(":components:chartview"))

    implementation(project(":subscriptions-core"))

    // UI Tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.espresso.core)

    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.powermock.api.mockito2)
    testImplementation(libs.powermock.module.junit4)
    testImplementation(libs.spek.dsl.jvm)
    testRuntimeOnly(libs.spek.runner.junit5)
    testRuntimeOnly(libs.kotlin.reflect)
}

// Flavor-specific dependencies must be added after evaluation
afterEvaluate {
    dependencies {
        "baseDebugImplementation"(project(":subscriptions-dev"))
        "baseReleaseImplementation"(project(":subscriptions-google-play"))

        "fdroidImplementation"(project(":subscriptions-fdroid"))
        "fdroidCiImplementation"(project(":subscriptions-fdroid"))
        "ciImplementation"(project(":subscriptions-dev"))
    }
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, TimeUnit.SECONDS)

        // Force Ktor version for TonKit
        force("io.ktor:ktor-utils:2.3.7")
        force("io.ktor:ktor-io:2.3.7")
        force("io.ktor:ktor-client-core:2.3.7")
    }
}
