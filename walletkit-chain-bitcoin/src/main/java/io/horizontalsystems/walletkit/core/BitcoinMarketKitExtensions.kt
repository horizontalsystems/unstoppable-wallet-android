package io.horizontalsystems.walletkit.core

import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.marketkit.models.TokenType

val TokenType.AddressType.kitCoinType: MainNetBitcoinCash.CoinType
    get() = when (this) {
        TokenType.AddressType.Type0 -> MainNetBitcoinCash.CoinType.Type0
        TokenType.AddressType.Type145 -> MainNetBitcoinCash.CoinType.Type145
    }
