plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose) // MainActivity hosts the Compose nav entry

    // Optional, provides the @Serialize annotation for autogeneration of Serializers.
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// buildConfigField expects a Java expression, so plain strings need embedded quotes
fun com.android.build.api.dsl.VariantDimension.buildConfigFieldString(name: String, value: String) {
    buildConfigField("String", name, "\"$value\"")
}

android {
    namespace = "io.horizontalsystems.bankwallet"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.horizontalsystems.bankwallet"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 173
        versionName = "0.50.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations += listOf("de", "es", "en", "fa", "fr", "ko", "pt", "pt-rBR", "ru", "tr", "zh")

        vectorDrawables.useSupportLibrary = true

        buildConfigFieldString("COMPANY_WEB_PAGE_LINK", "https://horizontalsystems.io")
        buildConfigFieldString("APP_WEB_PAGE_LINK", "https://unstoppable.money")
        buildConfigFieldString("ANALYTICS_LINK", "https://unstoppable.money/analytics")
        buildConfigFieldString("APP_GITHUB_LINK", "https://github.com/horizontalsystems/unstoppable-wallet-android")
        buildConfigFieldString("APP_TWITTER_LINK", "https://twitter.com/UnstoppableByHS")
        buildConfigFieldString("APP_TELEGRAM_LINK", "https://t.me/unstoppable_announcements")
        buildConfigFieldString("REPORT_EMAIL", "support.unstoppable@protonmail.com")
        buildConfigFieldString("RELEASE_NOTES_URL", "https://api.github.com/repos/horizontalsystems/unstoppable-wallet-android/releases/tags/")
        buildConfigFieldString("WALLET_CONNECT_APP_META_DATA_NAME", "Unstoppable")
        buildConfigFieldString("WALLET_CONNECT_APP_META_DATA_URL", "unstoppable.money")
        buildConfigFieldString("WALLET_CONNECT_APP_META_DATA_ICON", "https://raw.githubusercontent.com/horizontalsystems/HS-Design/master/PressKit/UW-AppIcon-on-light.png")
        buildConfigFieldString("THORCHAIN_API_KEY", "THORCHAIN_APIJ4R0S3UWZI8GEXTQ")

        buildConfigField("boolean", "FDROID_BUILD", "false")

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("test") {
            storeFile = file("./test.keystore")
            storePassword = "testKeystore123"
            keyAlias = "testKeystore"
            keyPassword = "testKeystore123"
        }
    }

    flavorDimensions += "distribution"

    val uswapApiKeyAndroid = "a32d6d05ef80c878c49eb7692aa6e2b36c4c0c7777b89e2c3c4d8e512a7cea61"
    val uswapApiKeyFdroid = "6e928d1db31e481ae57d42f34a9b7d58a64d7e2380f7ea696e652bd9a0ee516e"
    val oneInchFeeAddressAndroid = "0xe42BBeE8389548fAe35C09072065b7fEc582b590"
    val oneInchFeeAddressFdroid = "0x8009267B9929196f74720F2f1496bbD7B79945F1"

    productFlavors {
        create("base") {
            dimension = "distribution"
            buildConfigFieldString("USWAP_API_KEY", uswapApiKeyAndroid)
            buildConfigFieldString("ONE_INCH_PARTNER_FEE_ADDRESS", oneInchFeeAddressAndroid)
        }

        create("fdroid") {
            dimension = "distribution"
            buildConfigField("boolean", "FDROID_BUILD", "true")
            buildConfigFieldString("USWAP_API_KEY", uswapApiKeyFdroid)
            buildConfigFieldString("ONE_INCH_PARTNER_FEE_ADDRESS", oneInchFeeAddressFdroid)
        }

        create("fdroidCi") {
            dimension = "distribution"
            applicationIdSuffix = ".fdroidci"
            buildConfigField("boolean", "FDROID_BUILD", "true")
            signingConfig = signingConfigs.getByName("test")
            buildConfigFieldString("USWAP_API_KEY", uswapApiKeyFdroid)
            buildConfigFieldString("ONE_INCH_PARTNER_FEE_ADDRESS", oneInchFeeAddressFdroid)
        }

        create("ci") {
            dimension = "distribution"
            applicationIdSuffix = ".appcenter"
            versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: defaultConfig.versionCode
            signingConfig = signingConfigs.getByName("test")
            buildConfigFieldString("APP_LINKS_HOST", "dev.unstoppable.money")
            manifestPlaceholders["appLinksHost"] = "dev.unstoppable.money"
            buildConfigFieldString("USWAP_API_KEY", uswapApiKeyAndroid)
            buildConfigFieldString("ONE_INCH_PARTNER_FEE_ADDRESS", oneInchFeeAddressAndroid)
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("test")
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".dev"
            buildConfigFieldString("APP_LINKS_HOST", "dev.unstoppable.money")
            manifestPlaceholders["appLinksHost"] = "dev.unstoppable.money"
            buildConfigFieldString("TWITTER_BEARER_TOKEN", "AAAAAAAAAAAAAAAAAAAAAJgeNwEAAAAA6xVpR6xLKTrxIA3kkSyRA92LDpA%3Da6auybDwcymUyh2BcS6zZwicUdxGtrzJC0qvOSdRwKLeqBGhwB")
            buildConfigFieldString("ETHERSCAN_KEY", "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE")
            buildConfigFieldString("BSCSCAN_KEY", "R396MSJNCKX2YK4EIMP3EWYAW21NSVMXRN")
            buildConfigFieldString("OTHER_SCAN_KEY", "FU7CYEXQEUSMXJJF8MZR6BNRMP9XT8S9CP")
            buildConfigFieldString("GUIDES_URL", "https://raw.githubusercontent.com/horizontalsystems/blockchain-crypto-guides/v1.2/index.json")
            buildConfigFieldString("EDU_URL", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/refs/tags/v1.4/src/edu.json")
            buildConfigFieldString("FAQ_URL", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/master/src/faq.json")
            buildConfigFieldString("COINS_JSON_URL", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/master/coins.json")
            buildConfigFieldString("PROVIDER_COINS_JSON_URL", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/master/provider.coins.json")
            buildConfigFieldString("MARKET_API_BASE_URL", "https://api-dev.blocksdecoded.com")
            buildConfigFieldString("MARKET_API_KEY", "IQf1uAjkthZp1i2pYzkXFDom")
            buildConfigFieldString("OPEN_SEA_API_KEY", "bfbd6061a33e455c8581b594774fecb3")
            buildConfigFieldString("WALLET_CONNECT_V2_KEY", "8b4f41c60880a3e3ad57d82fddb30568")
            buildConfigFieldString("SOLANA_ALCHEMY_API_KEY", "PKgWxOMarrHgyMESGjIkJ,BOlzgqJUeGYe5E7K613Fm")
            buildConfigFieldString("SOLANA_JUPITER_API_KEY", "ec901a97-0375-45b1-8b7d-da1ea9934cb0")
            buildConfigFieldString("TRONGRID_API_KEYS", "33374494-8060-447e-8367-90c5efd4ed95")
            buildConfigFieldString("UDN_API_KEY", "r2phzgatt_zt9-hd_wyvdjrdsrimnxgokm7knyag1malzgcz")
            buildConfigFieldString("ONE_INCH_API_KEY", "3EttyCzgWb2GLFIRoPIUYM0M4uKAVEcq")
            buildConfigFieldString("BLOCKS_DECODED_ETHEREUM_RPC", "https://api-dev.blocksdecoded.com/v1/ethereum-rpc/mainnet")
            buildConfigFieldString("CHAINALYSIS_BASE_URL", "https://public.chainalysis.com/api/v1/")
            buildConfigFieldString("CHAINALYSIS_API_KEY", "928bb256db73f1cb93e1b3366a145d9fbe06e28581c8b665b82ad70bbfef1db4")
            buildConfigFieldString("HASH_DIT_BASE_URL", "https://service.hashdit.io/v2/hashdit/")
            buildConfigFieldString("HASH_DIT_API_KEY", "aGMkgODYiUFtTYrSRcEZsIfPHeASOlGYXClJZNWF")
            buildConfigFieldString("USWAP_API_BASE_URL", "https://swap-dev.unstoppable.money/api/v2/")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigFieldString("APP_LINKS_HOST", "unstoppable.money")
            manifestPlaceholders["appLinksHost"] = "unstoppable.money"
            buildConfigFieldString("TWITTER_BEARER_TOKEN", "AAAAAAAAAAAAAAAAAAAAAJgeNwEAAAAA6xVpR6xLKTrxIA3kkSyRA92LDpA%3Da6auybDwcymUyh2BcS6zZwicUdxGtrzJC0qvOSdRwKLeqBGhwB")
            buildConfigFieldString("ETHERSCAN_KEY", "IEXTB9RE7MUV2UQ9X238RP146IEJB1J5HS,27S4V3GYJGMCPWQZ2T4SF9355QBQYQ3FI7,YK4KEA3TANM8KZ5J6E2Q1ZIM6YDM8TEABM,FU7CYEXQEUSMXJJF8MZR6BNRMP9XT8S9CP")
            buildConfigFieldString("BSCSCAN_KEY", "FQ2HSNNEHVG71U96P1TF3WF9RTF6AF5MRA,G6K8VZDWYSJHTCRURRITFZ2ZWV48GRGTZQ,R396MSJNCKX2YK4EIMP3EWYAW21NSVMXRN,8QW2JNMPHPUPAACFGXZ3A5PVQY6PBCJPEG")
            buildConfigFieldString("OTHER_SCAN_KEY", "Y855XHV4XKUC9DTRM2ZQG8XAQ96EJV221Q,43DEJEEMA1P81YAU555A1TECRY5FPIWCFH")
            buildConfigFieldString("GUIDES_URL", "https://raw.githubusercontent.com/horizontalsystems/blockchain-crypto-guides/v1.2/index.json")
            buildConfigFieldString("EDU_URL", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/refs/tags/v1.4/src/edu.json")
            buildConfigFieldString("FAQ_URL", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/v1.3/src/faq.json")
            buildConfigFieldString("COINS_JSON_URL", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/v0.21/coins.json")
            buildConfigFieldString("PROVIDER_COINS_JSON_URL", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/v0.21/provider.coins.json")
            buildConfigFieldString("MARKET_API_BASE_URL", "https://api.blocksdecoded.com")
            buildConfigFieldString("MARKET_API_KEY", "IQf1uAjkthZp1i2pYzkXFDom")
            buildConfigFieldString("OPEN_SEA_API_KEY", "bfbd6061a33e455c8581b594774fecb3")
            buildConfigFieldString("WALLET_CONNECT_V2_KEY", "0c5ca155c2f165a7d0c88686f2113a72")
            buildConfigFieldString("SOLANA_ALCHEMY_API_KEY", "BOlzgqJUeGYe5E7K613Fm,Vmt7ucAGIMEux_c43Qqqf,uCordWq3EOD800awDx1kb,1uAryzn6DOEVs5PIugeoR,PKgWxOMarrHgyMESGjIkJ")
            buildConfigFieldString("SOLANA_JUPITER_API_KEY", "ec901a97-0375-45b1-8b7d-da1ea9934cb0")
            buildConfigFieldString("TRONGRID_API_KEYS", "8f5ae2c8-8012-42a8-b0ca-ffc2741f6a29,578aa64f-a79f-4ee8-86e9-e9860e2d050a,1e92f1fc-41f8-401f-a7f6-5b719b6f1280,d1511874-1547-48df-9536-a32cc85949ac")
            buildConfigFieldString("UDN_API_KEY", "r2phzgatt_zt9-hd_wyvdjrdsrimnxgokm7knyag1malzgcz")
            buildConfigFieldString("ONE_INCH_API_KEY", "3EttyCzgWb2GLFIRoPIUYM0M4uKAVEcq")
            buildConfigFieldString("BLOCKS_DECODED_ETHEREUM_RPC", "https://api.blocksdecoded.com/v1/ethereum-rpc/mainnet")
            buildConfigFieldString("CHAINALYSIS_BASE_URL", "https://public.chainalysis.com/api/v1/")
            buildConfigFieldString("CHAINALYSIS_API_KEY", "928bb256db73f1cb93e1b3366a145d9fbe06e28581c8b665b82ad70bbfef1db4")
            buildConfigFieldString("HASH_DIT_BASE_URL", "https://service.hashdit.io/v2/hashdit/")
            buildConfigFieldString("HASH_DIT_API_KEY", "aGMkgODYiUFtTYrSRcEZsIfPHeASOlGYXClJZNWF")
            buildConfigFieldString("USWAP_API_BASE_URL", "https://swap-api.unstoppable.money/v2/")
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
        dex {
            useLegacyPackaging = true
        }
    }

    lint {
        disable += "LogNotTimber"
        disable += "RemoveWorkManagerInitializer"
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
    // Everything else now lives in :walletkit, which re-exports its dependencies (api),
    // so the thin :app shell only needs :walletkit plus the flavor/test bits below.
    implementation(project(":walletkit"))
    implementation(project(":walletkit-chain-zano"))
    implementation(project(":walletkit-chain-monero"))
    implementation(project(":walletkit-chain-zcash"))
    implementation(project(":walletkit-chain-solana"))
    implementation(project(":walletkit-chain-stellar"))
    implementation(project(":walletkit-chain-ton"))
    implementation(libs.androidx.splashscreen) // MainActivity installs the splash screen
    debugImplementation(libs.leakcanary)

    // Desugar
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Exclude old version from wherever it's coming
    configurations.configureEach {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
        exclude(group = "org.bouncycastle", module = "bcutil-jdk18on")
    }

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
        findProject(":subscriptions-google-play")?.let {
            "baseReleaseImplementation"(it)
        }

        "fdroidImplementation"(project(":subscriptions-fdroid"))
        "fdroidCiImplementation"(project(":subscriptions-fdroid"))
        "ciImplementation"(project(":subscriptions-dev"))

        findProject(":dapp-wallet-connect")?.let {
            "baseDebugImplementation"(it)
            "baseReleaseImplementation"(it)
            "ciImplementation"(it)
        }

        "baseDebugImplementation"(libs.androidx.credentials.play.services.auth)
        "baseReleaseImplementation"(libs.androidx.credentials.play.services.auth)
        "ciImplementation"(libs.androidx.credentials.play.services.auth)
    }
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, TimeUnit.SECONDS)
        // PowerMock forces junit:4.12 which conflicts with androidTest deps requiring 4.13.2
        force("junit:junit:4.13.2")

        // Force Ktor version for TonKit
        force("io.ktor:ktor-utils:2.3.7")
        force("io.ktor:ktor-io:2.3.7")
        force("io.ktor:ktor-client-core:2.3.7")
    }
}

// Forward -PupdateParityFixture=true to the test JVM so ChainBehaviorParityTest can
// regenerate its golden fixture (see walletkit/docs/Walletkit-Modularization-Plan.md).
tasks.withType<Test>().configureEach {
    systemProperty(
        "updateParityFixture",
        providers.gradleProperty("updateParityFixture").getOrElse("false")
    )
}
