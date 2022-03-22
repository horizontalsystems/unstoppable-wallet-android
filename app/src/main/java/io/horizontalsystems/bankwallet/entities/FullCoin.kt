package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Platform

val FullCoin.supportedPlatforms: List<Platform>
    get() = platforms.filter { it.coinType.isSupported }
