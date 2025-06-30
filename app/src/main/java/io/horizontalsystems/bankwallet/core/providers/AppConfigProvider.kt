package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

class AppConfigProvider(localStorage: ILocalStorage) {

    val appId by lazy { localStorage.appId }
    val appVersion by lazy { BuildConfig.VERSION_NAME }
    val appBuild by lazy { BuildConfig.VERSION_CODE }
    val companyWebPageLink by lazy { Translator.getString(R.string.companyWebPageLink) }
    val appWebPageLink by lazy { Translator.getString(R.string.appWebPageLink) }
    val analyticsLink by lazy { Translator.getString(R.string.analyticsLink) }
    val appGithubLink by lazy { Translator.getString(R.string.appGithubLink) }
    val appTwitterLink by lazy { Translator.getString(R.string.appTwitterLink) }
    val appTelegramLink by lazy { Translator.getString(R.string.appTelegramLink) }
    val reportEmail by lazy { Translator.getString(R.string.reportEmail) }
    val releaseNotesUrl by lazy { Translator.getString(R.string.releaseNotesUrl) }
    val mempoolSpaceUrl: String = "https://mempool.space"
    val walletConnectUrl = "relay.walletconnect.com"
    val walletConnectProjectId by lazy { Translator.getString(R.string.walletConnectV2Key) }
    val walletConnectAppMetaDataName by lazy { Translator.getString(R.string.walletConnectAppMetaDataName) }
    val walletConnectAppMetaDataUrl by lazy { Translator.getString(R.string.walletConnectAppMetaDataUrl) }
    val walletConnectAppMetaDataIcon by lazy { Translator.getString(R.string.walletConnectAppMetaDataIcon) }
    val accountsBackupFileSalt by lazy { Translator.getString(R.string.accountsBackupFileSalt) }

    val blocksDecodedEthereumRpc by lazy {
        Translator.getString(R.string.blocksDecodedEthereumRpc)
    }
    val twitterBearerToken by lazy {
        Translator.getString(R.string.twitterBearerToken)
    }
    val etherscanApiKey by lazy {
        Translator.getString(R.string.etherscanKey).split(",")
    }
    val bscscanApiKey by lazy {
        Translator.getString(R.string.bscscanKey).split(",")
    }
    val polygonscanApiKey by lazy {
        Translator.getString(R.string.polygonscanKey).split(",")
    }
    val snowtraceApiKey by lazy {
        Translator.getString(R.string.snowtraceApiKey).split(",")
    }
    val optimisticEtherscanApiKey by lazy {
        Translator.getString(R.string.optimisticEtherscanApiKey).split(",")
    }
    val arbiscanApiKey by lazy {
        Translator.getString(R.string.arbiscanApiKey).split(",")
    }
    val gnosisscanApiKey by lazy {
        Translator.getString(R.string.gnosisscanApiKey).split(",")
    }
    val ftmscanApiKey by lazy {
        Translator.getString(R.string.ftmscanApiKey).split(",")
    }
    val basescanApiKey by lazy {
        Translator.getString(R.string.basescanApiKey).split(",")
    }
    val eraZkSyncApiKey by lazy {
        Translator.getString(R.string.eraZkSyncApiKey).split(",")
    }
    val guidesUrl by lazy {
        Translator.getString(R.string.guidesUrl)
    }
    val eduUrl by lazy {
        Translator.getString(R.string.eduUrl)
    }
    val faqUrl by lazy {
        Translator.getString(R.string.faqUrl)
    }
    val coinsJsonUrl by lazy {
        Translator.getString(R.string.coinsJsonUrl)
    }
    val providerCoinsJsonUrl by lazy {
        Translator.getString(R.string.providerCoinsJsonUrl)
    }

    val marketApiBaseUrl by lazy {
        Translator.getString(R.string.marketApiBaseUrl)
    }

    val marketApiKey by lazy {
        Translator.getString(R.string.marketApiKey)
    }

    val openSeaApiKey by lazy {
        Translator.getString(R.string.openSeaApiKey)
    }

    val solscanApiKey by lazy {
        Translator.getString(R.string.solscanApiKey)
    }

    val trongridApiKeys: List<String> by lazy {
        Translator.getString(R.string.trongridApiKeys).split(",")
    }

    val udnApiKey by lazy {
        Translator.getString(R.string.udnApiKey)
    }

    val oneInchApiKey by lazy {
        Translator.getString(R.string.oneInchApiKey)
    }

    val fiatDecimal: Int = 2
    val feeRateAdjustForCurrencies: List<String> = listOf("USD", "EUR")

    val currencies: List<Currency> = listOf(
        Currency("AUD", "A$", 2, R.drawable.icon_32_flag_australia),
        Currency("ARS", "$", 2, R.drawable.icon_32_flag_argentine),
        Currency("BRL", "R$", 2, R.drawable.icon_32_flag_brazil),
        Currency("CAD", "C$", 2, R.drawable.icon_32_flag_canada),
        Currency("CHF", "₣", 2, R.drawable.icon_32_flag_switzerland),
        Currency("CNY", "¥", 2, R.drawable.icon_32_flag_china),
        Currency("EUR", "€", 2, R.drawable.icon_32_flag_europe),
        Currency("GBP", "£", 2, R.drawable.icon_32_flag_england),
        Currency("HKD", "HK$", 2, R.drawable.icon_32_flag_hongkong),
        Currency("HUF", "Ft", 2, R.drawable.icon_32_flag_hungary),
        Currency("ILS", "₪", 2, R.drawable.icon_32_flag_israel),
        Currency("INR", "₹", 2, R.drawable.icon_32_flag_india),
        Currency("JPY", "¥", 2, R.drawable.icon_32_flag_japan),
        Currency("NOK", "kr", 2, R.drawable.icon_32_flag_norway),
        Currency("PHP", "₱", 2, R.drawable.icon_32_flag_philippine),
        Currency("RUB", "₽", 2, R.drawable.icon_32_flag_russia),
        Currency("SGD", "S$", 2, R.drawable.icon_32_flag_singapore),
        Currency("USD", "$", 2, R.drawable.icon_32_flag_usa),
        Currency("ZAR", "R", 2, R.drawable.icon_32_flag_south_africa),
    )

    val donateAddresses: Map<BlockchainType, String> by lazy {
        if (BuildConfig.FDROID_BUILD) {
            mapOf(
                BlockchainType.Bitcoin to "bc1qy3ekl877sll3pzw9ramknx0wgyxfhzlccv940y",
                BlockchainType.BitcoinCash to "bitcoincash:qq0uv2s4nd8g7htpnsp77pwjpfdyrvgncu3tfk252s",
                BlockchainType.ECash to "ecash:qznldf5phm5dgq6rd2dpxd4qcypm32dyqsgux0v94h",
                BlockchainType.Litecoin to "ltc1q8x0zcezuz82mhhmclpa2hv2jf8vry66ap08d2w",
                BlockchainType.Dash to "XcHq4AuSC2CMQuf8wLojDJ9QEFQJShzgvf",
                BlockchainType.Zcash to "zs1rqg09d8t6utx3znyrzw4jz0y3tdh633yqychhumsm4z69y4dcye9z9aghpvpygzgktnexwnzagl",
                BlockchainType.Ethereum to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.BinanceSmartChain to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Polygon to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Avalanche to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Optimism to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Base to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.ZkSync to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.ArbitrumOne to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Solana to "EKQVqxaXVJf1QaVUeNynKkJC7rT4abMnWtg5TtqY2S5F",
                BlockchainType.Gnosis to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Fantom to "0x2174BFA51C4c5ADa3035f3a9ccEb5DbeE32EE162",
                BlockchainType.Ton to "UQA94iEyQI0iVD0ssowbHGizBEY5uMm9tMz72IecYjA_nnZG",
                BlockchainType.Tron to "TXkwDeqz77793xYJqxCHuEPiqqj8B8Cf2Z",
            ).toList().sortedBy { (key, _) -> key.order }.toMap()
        } else {
            mapOf(
                BlockchainType.Bitcoin to "bc1qy0dy3ufpup9eyeprnd8a6fe2scg2m4rr4peasy",
                BlockchainType.BitcoinCash to "bitcoincash:qqlwaf0vrvq722pta5jfc83m6cv7569nzya0ry6prk",
                BlockchainType.ECash to "ecash:qp9cqsjfttdv2x9y0el3ghk7xy4dy07p6saz7w2xvq",
                BlockchainType.Litecoin to "ltc1qtnyd4vq4yvu4g00jd3nl25w8qftj32dvfanyfx",
                BlockchainType.Dash to "XqCrPRKwBeW4pNPbNUTQTsnKQ626RNz4no",
                BlockchainType.Zcash to "zs1r9gf53xg3206g7wlhwwq7lcdrtzalepnvk7kwpm8yxr0z3ng0y898scd505rsekj8c4xgwddz4m",
                BlockchainType.Ethereum to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.BinanceSmartChain to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Polygon to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Avalanche to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Optimism to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Base to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.ZkSync to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.ArbitrumOne to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Solana to "ELFQmFXqdS6C1zVqZifs7WAmLKovdEPbWSnqomhZoK3B",
                BlockchainType.Gnosis to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Fantom to "0x731352dcF66014156B1560B832B56069e7b38ab1",
                BlockchainType.Ton to "UQDgkDkU_3Mtujk2FukZEsiXV9pOhVzkdvvYH8es0tZylTZY",
                BlockchainType.Tron to "TXKA3SxjLsUL4n6j3v2h85fzb4V7Th6yh6",
            ).toList().sortedBy { (key, _) -> key.order }.toMap()
        }
    }

    // coinCode -> min value
    val spamCoinValueLimits: Map<String, BigDecimal> = mapOf(
        "USDT" to BigDecimal("0.01"),
        "USDC" to BigDecimal("0.01"),
        "DAI" to BigDecimal("0.01"),
        "BUSD" to BigDecimal("0.01"),
        "EURS" to BigDecimal("0.01"),
        "BSC-USD" to BigDecimal("0.01"),
        "TRX" to BigDecimal("10"),
        "XLM" to BigDecimal("0.01"),
    )

    val chainalysisBaseUrl by lazy {
        Translator.getString(R.string.chainalysisBaseUrl)
    }

    val chainalysisApiKey by lazy {
        Translator.getString(R.string.chainalysisApiKey)
    }

    val hashDitBaseUrl by lazy {
        Translator.getString(R.string.hashDitBaseUrl)
    }

    val hashDitApiKey by lazy {
        Translator.getString(R.string.hashDitApiKey)
    }

}
