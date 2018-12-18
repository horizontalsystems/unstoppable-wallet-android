package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

sealed class CoinType {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Ethereum : CoinType()
    class Erc20(address: String, decimal: Int) : CoinType()
}

data class Coin(val title: String, val code: CoinCode, val type: CoinType)
