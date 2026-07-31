package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.order
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

class AppConfigProvider(localStorage: ILocalStorage) : IAppConfigProvider {

    override val appId by lazy { localStorage.appId }
    override val appVersion by lazy { BuildConfig.VERSION_NAME }
    override val appBuild by lazy { BuildConfig.VERSION_CODE }
    override val companyWebPageLink = BuildConfig.COMPANY_WEB_PAGE_LINK
    override val appWebPageLink = BuildConfig.APP_WEB_PAGE_LINK
    override val analyticsLink = BuildConfig.ANALYTICS_LINK
    override val appGithubLink = BuildConfig.APP_GITHUB_LINK
    override val appTwitterLink = BuildConfig.APP_TWITTER_LINK
    override val appTelegramLink = BuildConfig.APP_TELEGRAM_LINK
    override val reportEmail = BuildConfig.REPORT_EMAIL
    override val releaseNotesUrl = BuildConfig.RELEASE_NOTES_URL
    override val mempoolSpaceUrl: String = "https://mempool.space"
    override val blockCypherUrl: String = "https://api.blockcypher.com"
    override val walletConnectUrl = "relay.walletconnect.com"
    override val walletConnectProjectId = BuildConfig.WALLET_CONNECT_V2_KEY
    override val walletConnectAppMetaDataName = BuildConfig.WALLET_CONNECT_APP_META_DATA_NAME
    override val walletConnectAppMetaDataUrl = BuildConfig.WALLET_CONNECT_APP_META_DATA_URL
    override val walletConnectAppMetaDataIcon = BuildConfig.WALLET_CONNECT_APP_META_DATA_ICON
    override val accountsBackupFileSalt = BuildConfig.ACCOUNTS_BACKUP_FILE_SALT
    override val simplexSupportChat = "https://smp11.simplex.im/g#yTrDh716RZCNYsdPSDrqMMlHnqZlW4XJGnFTugBrsAI"
    override val nymVpnLink = "https://nymtechnologies.pxf.io/N9vnr1"
    override val telegramSupportChat = "https://t.me/m/1TNZ9JE4MTNi"

    override val blocksDecodedEthereumRpc = BuildConfig.BLOCKS_DECODED_ETHEREUM_RPC
    override val twitterBearerToken = BuildConfig.TWITTER_BEARER_TOKEN
    override val etherscanApiKey = BuildConfig.ETHERSCAN_KEY.split(",")
    override val bscscanApiKey = BuildConfig.BSCSCAN_KEY.split(",")
    override val otherScanApiKey = BuildConfig.OTHER_SCAN_KEY.split(",")
    override val guidesUrl = BuildConfig.GUIDES_URL
    override val eduUrl = BuildConfig.EDU_URL
    override val faqUrl = BuildConfig.FAQ_URL
    override val coinsJsonUrl = BuildConfig.COINS_JSON_URL
    override val providerCoinsJsonUrl = BuildConfig.PROVIDER_COINS_JSON_URL
    override val marketApiBaseUrl = BuildConfig.MARKET_API_BASE_URL
    override val marketApiKey = BuildConfig.MARKET_API_KEY
    override val openSeaApiKey = BuildConfig.OPEN_SEA_API_KEY
    override val solanaAlchemyApiKey = BuildConfig.SOLANA_ALCHEMY_API_KEY
    override val solanaJupiterApiKey = BuildConfig.SOLANA_JUPITER_API_KEY
    override val trongridApiKeys: List<String> = BuildConfig.TRONGRID_API_KEYS.split(",")
    override val udnApiKey = BuildConfig.UDN_API_KEY
    override val oneInchApiKey = BuildConfig.ONE_INCH_API_KEY
    override val thorchainApiKey = BuildConfig.THORCHAIN_API_KEY
    override val appLinksHost = BuildConfig.APP_LINKS_HOST

    override val fiatDecimal: Int = 2
    override val feeRateAdjustForCurrencies: List<String> = listOf("USD", "EUR")

    override val currencies: List<Currency> = listOf(
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

    override val donateAddresses: Map<BlockchainType, String> by lazy {
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
                BlockchainType.Monero to "4B5Dc1VFUpsVxDu8d8y8r44FQAjfmaSL4c3SSydUATpJPGsMkV4qswkenLAY4g9wm98bsvskVZXgDWgW2jA1t31MNcCZ8AZ",
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
                BlockchainType.Monero to "46ZLVbtaBZFBdztK3L2sJEEwhvKL9B5jbEjBtJWS5DLTAsBS7K4KBpHU3M738qvVcZ1ejUoFichxubCnHLwvGQnu2SWtoeK"
            ).toList().sortedBy { (key, _) -> key.order }.toMap()
        }
    }

    // coinCode -> risk threshold (used for dust detection scoring)
    // spam = risk/10 (auto-spam), risk = config value (+3 points), danger = risk*5 (+2 points)
    override val spamCoinValueLimits: Map<String, BigDecimal> = mapOf(
        "XLM" to BigDecimal("0.1"),
        "USDT" to BigDecimal("1"),
        "USDC" to BigDecimal("1"),
        "USDD" to BigDecimal("1"),
        "DAI" to BigDecimal("1"),
        "BUSD" to BigDecimal("1"),
        "EURS" to BigDecimal("1"),
        "BSC-USD" to BigDecimal("1"),
        "TRX" to BigDecimal("1"),
        "ETH" to BigDecimal("0.0005"),
        "POL" to BigDecimal("1"),
        "BNB" to BigDecimal("0.0002"),
        "SOL" to BigDecimal("0.000001"),
    )

    override val chainalysisBaseUrl = BuildConfig.CHAINALYSIS_BASE_URL

    override val chainalysisApiKey = BuildConfig.CHAINALYSIS_API_KEY

    override val hashDitBaseUrl = BuildConfig.HASH_DIT_BASE_URL

    override val hashDitApiKey = BuildConfig.HASH_DIT_API_KEY

    override val uswapApiBaseUrl = BuildConfig.USWAP_API_BASE_URL

    override val uswapApiKey = BuildConfig.USWAP_API_KEY

    override val oneInchPartnerFeeAddress = BuildConfig.ONE_INCH_PARTNER_FEE_ADDRESS

    override val fdroidBuild: Boolean = BuildConfig.FDROID_BUILD
}
