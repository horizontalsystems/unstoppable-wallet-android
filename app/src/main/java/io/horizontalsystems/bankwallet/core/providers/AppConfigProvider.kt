package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Currency

class AppConfigProvider {

     val companyWebPageLink: String = "https://horizontalsystems.io"
     val appWebPageLink: String = "https://unstoppable.money"
     val analyticsLink: String = "https://unstoppable.money/analytics"
     val appGithubLink: String = "https://github.com/horizontalsystems/unstoppable-wallet-android"
     val appTwitterLink: String = "https://twitter.com/UnstoppableByHS"
     val appTelegramLink: String = "https://t.me/unstoppable_announcements"
     val appRedditLink: String = "https://www.reddit.com/r/UNSTOPPABLEWallet/"
     val reportEmail = "support.unstoppable@protonmail.com"
     val btcCoreRpcUrl: String = "https://btc.blocksdecoded.com/rpc"
     val releaseNotesUrl: String = "https://api.github.com/repos/horizontalsystems/unstoppable-wallet-android/releases/tags/"
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
    val ftmscanApiKey by lazy {
        Translator.getString(R.string.ftmscanApiKey)
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

    val trongridApiKey by lazy {
        Translator.getString(R.string.trongridApiKey)
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

}
