package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.xxxkit.models.Coin

fun Coin.toOld() : io.horizontalsystems.marketkit.models.Coin {
    return io.horizontalsystems.marketkit.models.Coin(
        uid = uid,
        name = name,
        code = code,
        marketCapRank = marketCapRank,
        coinGeckoId = coinGeckoId
    )
}
