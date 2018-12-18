package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Network

class AppConfigProvider : IAppConfigProvider {

    override val network: Network
        get() {
            val networkRaw = App.instance.getString(R.string.network_type)
            return Network.fromRawValue(networkRaw)
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

    override val localizations: List<String>
        get() {
            val coinsString = App.instance.getString(R.string.localizations)
            return coinsString.split(",")
        }
}
