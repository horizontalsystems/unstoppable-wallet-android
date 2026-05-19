package cash.p.terminal.core.providers

import cash.p.terminal.BuildConfig
import cash.p.terminal.R
import cash.p.terminal.network.data.EncodedSecrets
import io.horizontalsystems.core.entities.Currency
import cash.p.terminal.strings.helpers.Translator
import io.horizontalsystems.core.entities.BlockchainType
import java.math.BigDecimal

object AppConfigProvider {

    val appVersion by lazy { BuildConfig.VERSION_NAME }
    val appBuild by lazy { BuildConfig.VERSION_CODE }
    val appGitHash by lazy { BuildConfig.GIT_HASH }
    val appGitBranch by lazy { BuildConfig.GIT_BRANCH }
    val companyWebPageLink by lazy { Translator.getString(R.string.companyWebPageLink) }
    val appWebPageLink by lazy { Translator.getString(R.string.appWebPageLink) }
    val analyticsLink by lazy { Translator.getString(R.string.analyticsLink) }
    val appGithubLink by lazy { Translator.getString(R.string.appGithubLink) }
    val appTwitterLink by lazy { Translator.getString(R.string.appTwitterLink) }
    val appTelegramLink by lazy { Translator.getString(R.string.appTelegramLink) }
    val appRedditLink by lazy { Translator.getString(R.string.appRedditLink) }
    val reportEmail by lazy { Translator.getString(R.string.reportEmail) }
    const val mempoolSpaceUrl: String = "https://mempool.space"
    const val blockCypherUrl: String = "https://api.blockcypher.com"
    const val walletConnectUrl = "relay.walletconnect.com"
    val walletConnectProjectId by lazy { EncodedSecrets.WALLET_CONNECT_V2_KEY }
    val walletConnectAppMetaDataName by lazy { Translator.getString(R.string.walletConnectAppMetaDataName) }
    val walletConnectAppMetaDataUrl by lazy { Translator.getString(R.string.walletConnectAppMetaDataUrl) }
    val walletConnectAppMetaDataIcon by lazy { Translator.getString(R.string.walletConnectAppMetaDataIcon) }
    val accountsBackupFileSalt by lazy { Translator.getString(R.string.accountsBackupFileSalt) }

    val spamCoinValueLimits: Map<String, BigDecimal> = mapOf(
        "USDT" to BigDecimal("0.01"),
        "USDC" to BigDecimal("0.01"),
        "DAI" to BigDecimal("0.01"),
        "BUSD" to BigDecimal("0.01"),
        "EURS" to BigDecimal("0.01"),
        "BSC-USD" to BigDecimal("0.01"),
        "TRX" to BigDecimal("0.1"),
        "XLM" to BigDecimal("0.01"),
    )

    val blocksDecodedEthereumRpc by lazy {
        Translator.getString(R.string.blocksDecodedEthereumRpc)
    }
    val twitterBearerToken by lazy {
        EncodedSecrets.TWITTER_BEARER_TOKEN
    }
    val etherscanApiKey by lazy {
        EncodedSecrets.ETHERSCAN_KEY.split(",").map { it.trim() }
    }
    val otherScanApiKey by lazy {
        EncodedSecrets.OTHER_SCAN_KEY.split(",").map { it.trim() }
    }
    val bscscanApiKey by lazy {
        EncodedSecrets.BSCSCAN_KEY.split(",").map { it.trim() }
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
        EncodedSecrets.OPEN_SEA_API_KEY
    }

    val solscanApiKey by lazy {
        EncodedSecrets.SOLSCAN_API_KEY
    }

    val trongridApiKeys: List<String> by lazy {
        EncodedSecrets.TRONGRID_API_KEYS.split(",").map { it.trim() }
    }

    val udnApiKey by lazy {
        EncodedSecrets.UDN_API_KEY
    }

    val oneInchApiKey by lazy {
        EncodedSecrets.ONE_INCH_API_KEY
    }

    val chainalysisBaseUrl by lazy {
        Translator.getString(R.string.chainalysisBaseUrl)
    }

    val chainalysisApiKey by lazy {
        EncodedSecrets.CHAINALYSIS_API_KEY
    }

    val hashDitBaseUrl by lazy {
        Translator.getString(R.string.hashDitBaseUrl)
    }

    val hashDitApiKey by lazy {
        EncodedSecrets.HASH_DIT_API_KEY
    }

    val alphaAmlBaseUrl by lazy {
        Translator.getString(R.string.alphaAmlBaseUrl)
    }

    val alphaAmlApiKey by lazy {
        EncodedSecrets.ALPHA_AML_API_KEY
    }

    val premiumApiBaseUrl by lazy { BuildConfig.PREMIUM_API_BASE_URL }

    val merkleIoKey by lazy {
        EncodedSecrets.MERKLE_IO_KEY
    }

    val fiatDecimal: Int = 2

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
            BlockchainType.Bitcoin to "bc1q2ph64sryt6skegze6726fp98u44kjsc5exktap",
            BlockchainType.BitcoinCash to "bitcoincash:qp497gfxadpz30zxp7kltcq9c2jn57z34q5xuclrhf",
            BlockchainType.ECash to "ecash:qrzcal2fmm6vumxp3g2jndk0fepmt2racya9lc4yxy",
            BlockchainType.Litecoin to "LgevEuuQ8ht3rnd3YyQ6j88cXs49aCaoSU",
            BlockchainType.Cosanta to "Cbbp3meofT1ESU5p4d9ucXpXw9pxKCMEyi",
            BlockchainType.Dash to "Xv7U37XKp5d4fjvbeuganwhqXN7Sm4JJkt",
            BlockchainType.Dogecoin to "DUQx8grwwEgjmdC3WNrczxz33ub3S45x4q",
            BlockchainType.Zcash to "zs1hwyqs4mfrynq0ysjmhv8wuau5zam0gwpx8ujfv8epgyufkmmsp6t7cfk9y0th7qyx7fsc5azm08",
            BlockchainType.Ethereum to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.BinanceSmartChain to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Polygon to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.PirateCash to "PB2vfGqfagNb12DyYTZBYWGnreyt7E4Pug",
            BlockchainType.Avalanche to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Optimism to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Base to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.ZkSync to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.ArbitrumOne to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Solana to "CefzHT5zCUncm3yhTLck9bCRYkbjHrKToT1GpPUyqCMa",
            BlockchainType.Gnosis to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Fantom to "0x52be29951B0D10d5eFa48D58363a25fE5Cc097e9",
            BlockchainType.Ton to "UQCYTBH7n8OnQ6BgOfdkNRWF7socLJb9U-JMRcoz3UpL_0V6",
            BlockchainType.Tron to "TAaYtFBxJztC5pG1zMapvUFSfEyJLSLJM5",
            BlockchainType.Monero to "4AzdEoZxeGMFkdtAxaNLAZakqEVsWpVb2at4u6966WGDiXkS7ZPyi7haeThTGUAWXVKDTmQ9DYTWRHMjGVSBW82xRQqPxkg",
            BlockchainType.Stellar to "GAZXDMWYHMPM2WF6FCWEBIMJITKKTU6MLHYLCFRVB3WMXTNPVEHBOXRE"
        ).toList().sortedBy { it.first.uid }.toMap()
    }
}
