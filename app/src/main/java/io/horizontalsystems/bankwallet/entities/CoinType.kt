package io.horizontalsystems.bankwallet.entities

import java.io.Serializable
import java.math.BigDecimal

sealed class CoinType : Serializable {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Dash : CoinType()
    object Ethereum : CoinType()
    class Erc20(val address: String, val decimal: Int, val fee: BigDecimal = BigDecimal.ZERO) : CoinType()
}
