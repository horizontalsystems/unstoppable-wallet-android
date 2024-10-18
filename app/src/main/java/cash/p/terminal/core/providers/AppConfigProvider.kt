package cash.p.terminal.core.providers

import cash.p.terminal.BuildConfig
import cash.p.terminal.R
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.order
import cash.p.terminal.entities.Currency
import io.horizontalsystems.marketkit.models.BlockchainType

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
    val appRedditLink by lazy { Translator.getString(R.string.appRedditLink) }
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
    val guidesUrl by lazy {
        Translator.getString(R.string.guidesUrl)
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
        mapOf(
            BlockchainType.Bitcoin to "3G5fwc9PP9Lcb1y3RAYGzoQZs5enJkmdxN",
            BlockchainType.BitcoinCash to "bitcoincash:qr4f0pkvx86vv6cuae48nj83txqhwyt2fgadd9smxg",
            BlockchainType.ECash to "ecash:qrzcal2fmm6vumxp3g2jndk0fepmt2racya9lc4yxy",
            BlockchainType.Litecoin to "MNbHsci3A8u6UiqjBMMckXzfPrLjeMxdRC",
            BlockchainType.Dash to "XcpUrR8LkohMNB9TfJaC97id6boUhRU3wk",
            BlockchainType.Zcash to "zs1hwyqs4mfrynq0ysjmhv8wuau5zam0gwpx8ujfv8epgyufkmmsp6t7cfk9y0th7qyx7fsc5azm08",
            BlockchainType.Ethereum to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.BinanceSmartChain to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.BinanceChain to "bnb132w7sndlwn340jgqff2m9m4nsddx3hga55nx3l",
            BlockchainType.Polygon to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Avalanche to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Optimism to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Base to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.ArbitrumOne to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Solana to "CefzHT5zCUncm3yhTLck9bCRYkbjHrKToT1GpPUyqCMa",
            BlockchainType.Gnosis to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Fantom to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Ton to "UQCYTBH7n8OnQ6BgOfdkNRWF7socLJb9U-JMRcoz3UpL_0V6",
            BlockchainType.Tron to "TV4wYRcDun4iHb4oUgcse4Whptk9JKVui2"
        ).toList().sortedBy { (key, _) -> key.order }.toMap()
    }

}
