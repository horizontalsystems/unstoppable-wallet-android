plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    id(libs.plugins.devtools.ksp.get().pluginId)
}

android {
    namespace = "cash.p.terminal.wallet"
    compileSdk = 34

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "PIRATE_CONTRACT", "\"0xaFCC12e4040615E7Afe9fb4330eB3D9120acAC05\"")
        buildConfigField("String", "COSANTA_CONTRACT", "\"0x5F980533B994c93631A639dEdA7892fC49995839\"")
        minSdk = 27
    }

    buildTypes {
        debug {
            resValue("string", "marketApiBaseUrl", "https://api-dev.blocksdecoded.com")
            resValue("string", "marketApiKey", "IQf1uAjkthZp1i2pYzkXFDom")
        }
        release {
            resValue("string", "marketApiBaseUrl", "https://api.blocksdecoded.com")
            resValue("string", "marketApiKey", "IQf1uAjkthZp1i2pYzkXFDom")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.rxjava)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx2)

    implementation(libs.ton.kit)
    implementation(libs.bitcoin.kit)
    implementation(libs.ethereum.kit)
    implementation(libs.blockchain.fee.kit)
    implementation(libs.binance.kit)
    implementation(libs.tron.kit)
    implementation(libs.gson)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit.rxjava2)
    implementation(libs.retrofit.scalars)

    //room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.ui.text.android)
    ksp(libs.room.compiler)

    implementation(project(":core:strings"))
    implementation(project(":core:core"))
    implementation(project(":core:network"))
}