package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Currency

class AppConfigProvider : IAppConfigProvider {
    override val ipfsUrl = "https://ipfs.horizontalsystems.xyz/ipns/Qmd4Gv2YVPqs6dmSy1XEq7pQRSgLihqYKL2JjK7DMUFPVz/io-hs/data/"

    override val fiatDecimal: Int = 2
    override val maxDecimal: Int = 8

    override val testMode: Boolean = BuildConfig.testMode

    override val currencies: List<Currency> = listOf(
            Currency(code = "USD", symbol = "\u0024"),
            Currency(code = "EUR", symbol = "\u20AC"),
            Currency(code = "GBP", symbol = "\u00A3"),
            Currency(code = "JPY", symbol = "\u00A5"),
            Currency(code = "AUD", symbol = "\u20B3"),
            Currency(code = "CAD", symbol = "\u0024"),
            Currency(code = "CHF", symbol = "\u20A3"),
            Currency(code = "CNY", symbol = "\u00A5"),
            Currency(code = "KRW", symbol = "\u20AE"),
            Currency(code = "RUB", symbol = "\u20BD"),
            Currency(code = "TRY", symbol = "\u20BA")
    )

    override val localizations: List<String>
        get() {
            val coinsString = App.instance.getString(R.string.localizations)
            return coinsString.split(",")
        }

    override val defaultCoins: List<Coin>
        get() {
            val suffix = if (testMode) "t" else ""
            val coins = mutableListOf<Coin>()
            coins.add(Coin("Bitcoin", "BTC$suffix", CoinType.Bitcoin))
            coins.add(Coin("Bitcoin Cash", "BCH$suffix", CoinType.BitcoinCash))
            coins.add(Coin("Ethereum", "ETH$suffix", CoinType.Ethereum))
            coins.add(Coin("PundiX", "NPXS$suffix", CoinType.Erc20("0xA15C7Ebe1f07CaF6bFF097D8a589fb8AC49Ae5B3", 18)))
            return coins
        }
}
