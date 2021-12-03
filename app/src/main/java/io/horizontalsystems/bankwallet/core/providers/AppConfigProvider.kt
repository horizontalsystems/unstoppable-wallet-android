package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.entities.Currency

class AppConfigProvider {

     val companyWebPageLink: String = "https://horizontalsystems.io"
     val appWebPageLink: String = "https://unstoppable.money"
     val appGithubLink: String = "https://github.com/horizontalsystems/unstoppable-wallet-android"
     val appTwitterLink: String = "https://twitter.com/UnstoppableByHS"
     val appTelegramLink: String = "https://t.me/unstoppable_announcements"
     val appRedditLink: String = "https://www.reddit.com/r/UNSTOPPABLEWallet/"
     val reportEmail = "support.unstoppable@protonmail.com"
     val btcCoreRpcUrl: String = "https://btc.horizontalsystems.xyz/rpc"
     val releaseNotesUrl: String = "https://api.github.com/repos/horizontalsystems/unstoppable-wallet-android/releases/tags/"

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
     val maxDecimal: Int = 8
     val feeRateAdjustForCurrencies: List<String> = listOf("USD", "EUR")

     val currencies: List<Currency> = listOf(
            Currency("AUD", "A$", 2),
            Currency("BRL", "R$", 2),
            Currency("CAD", "C$", 2),
            Currency("CHF", "₣", 2),
            Currency("CNY", "¥", 2),
            Currency("EUR", "€", 2),
            Currency("GBP", "£", 2),
            Currency("HKD", "HK$", 2),
            Currency("ILS", "₪", 2),
            Currency("JPY", "¥", 2),
            Currency("RUB", "₽", 2),
            Currency("SGD", "S$", 2),
            Currency("USD", "$", 2),
    )

}
