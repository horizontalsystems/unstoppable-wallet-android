package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.coinkit.models.Coin

data class ConfiguredCoin(val coin: Coin, val settings: CoinSettings = CoinSettings())
