package cash.p.terminal.core.providers

import cash.p.terminal.R
import cash.p.terminal.entities.Currency

class AppConfigProvider {

     val companyWebPageLink: String = "https://p.cash/"
     val appWebPageLink: String = "https://p.cash/wallet"
     val appGithubLink: String = "https://github.com/piratecash/pcash-wallet-android"
     val appTwitterLink: String = "https://twitter.com/PirateCash_NET"
     val appTelegramLink: String = "https://t.me/pcash"
     val appRedditLink: String = "https://www.reddit.com/r/PirateCash/"
     val reportEmail = "i@p.cash"
     val btcCoreRpcUrl: String = "https://btc.blocksdecoded.com/rpc"
     val releaseNotesUrl: String = "https://api.github.com/repos/piratecash/pcash-wallet-android/releases/tags/"
     val walletConnectUrl = "relay.walletconnect.com"
     val walletConnectProjectId by lazy {
         Translator.getString(R.string.walletConnectV2Key)
     }

     val twitterBearerToken by lazy {
        Translator.getString(R.string.twitterBearerToken)
    }
     val cryptoCompareApiKey by lazy {
        Translator.getString(R.string.cryptoCompareApiKey)
    }
     val defiyieldProviderApiKey by lazy {
        Translator.getString(R.string.defiyieldProviderApiKey)
    }
     val infuraProjectId by lazy {
        Translator.getString(R.string.infuraProjectId)
    }
     val infuraProjectSecret by lazy {
        Translator.getString(R.string.infuraSecretKey)
    }
     val etherscanApiKey by lazy {
        Translator.getString(R.string.etherscanKey)
    }
     val bscscanApiKey by lazy {
        Translator.getString(R.string.bscscanKey)
    }
     val polygonscanApiKey by lazy {
        Translator.getString(R.string.polygonscanKey)
    }
     val snowtraceApiKey by lazy {
        Translator.getString(R.string.snowtraceApiKey)
    }
     val optimisticEtherscanApiKey by lazy {
        Translator.getString(R.string.optimisticEtherscanApiKey)
    }
     val arbiscanApiKey by lazy {
        Translator.getString(R.string.arbiscanApiKey)
    }
    val gnosisscanApiKey by lazy {
        Translator.getString(R.string.gnosisscanApiKey)
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

     val fiatDecimal: Int = 2
     val feeRateAdjustForCurrencies: List<String> = listOf("USD", "EUR")

    val currencies: List<Currency> = listOf(
        Currency("AUD", "A$", 2, R.drawable.icon_32_flag_australia),
        Currency("BRL", "R$", 2, R.drawable.icon_32_flag_brazil),
        Currency("CAD", "C$", 2, R.drawable.icon_32_flag_canada),
        Currency("CHF", "₣", 2, R.drawable.icon_32_flag_switzerland),
        Currency("CNY", "¥", 2, R.drawable.icon_32_flag_china),
        Currency("EUR", "€", 2, R.drawable.icon_32_flag_europe),
        Currency("GBP", "£", 2, R.drawable.icon_32_flag_england),
        Currency("HKD", "HK$", 2, R.drawable.icon_32_flag_hongkong),
        Currency("ILS", "₪", 2, R.drawable.icon_32_flag_israel),
        Currency("INR", "₹", 2, R.drawable.icon_32_flag_india),
        Currency("JPY", "¥", 2, R.drawable.icon_32_flag_japan),
        Currency("RUB", "₽", 2, R.drawable.icon_32_flag_russia),
        Currency("SGD", "S$", 2, R.drawable.icon_32_flag_singapore),
        Currency("USD", "$", 2, R.drawable.icon_32_flag_usa),
        Currency("ZAR", "R", 2, R.drawable.icon_32_flag_south_africa),
    )

}