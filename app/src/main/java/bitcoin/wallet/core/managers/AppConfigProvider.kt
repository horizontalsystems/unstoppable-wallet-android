package bitcoin.wallet.core.managers

import bitcoin.wallet.R
import bitcoin.wallet.core.App
import bitcoin.wallet.core.IAppConfigProvider
import bitcoin.wallet.entities.Currency

class AppConfigProvider : IAppConfigProvider {
    override val enabledCoins: List<String>
        get() {
            val coinsString = App.instance.getString(R.string.enabled_coins)
            return coinsString.split(",")
        }

    override val currencies: List<Currency> = listOf(
            Currency(code = "USD", symbol = "\u0024"),
            Currency(code = "EUR", symbol = "\u20AC"),
            Currency(code = "RUB", symbol = "\u20BD"),
            Currency(code = "AUD", symbol = "\u20B3"),
            Currency(code = "CAD", symbol = "\u0024"),
            Currency(code = "CHF", symbol = "\u20A3"),
            Currency(code = "CNY", symbol = "\u00A5"),
            Currency(code = "GBP", symbol = "\u00A3"),
            Currency(code = "JPY", symbol = "\u00A5")
    )

}
