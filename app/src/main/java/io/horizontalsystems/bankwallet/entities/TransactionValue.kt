package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal
import java.math.BigInteger

sealed class TransactionValue {
    abstract val coinName: String
    abstract val coinUid: String
    abstract val coinCode: String
    abstract val coin: Coin?
    abstract val decimalValue: BigDecimal?
    abstract val decimals: Int?
    abstract val zeroValue: Boolean
    abstract val isMaxValue: Boolean
    abstract val abs: TransactionValue
    abstract val formattedString: String

    data class CoinValue(val platformCoin: PlatformCoin, val value: BigDecimal) : TransactionValue() {
        override val coin: Coin = platformCoin.coin
        override val coinUid: String = coin.uid
        override val coinName: String = coin.name
        override val coinCode: String = coin.code
        override val decimalValue: BigDecimal = value
        override val decimals: Int = platformCoin.decimals
        override val zeroValue: Boolean
            get() = value.compareTo(BigDecimal.ZERO) == 0
        override val isMaxValue: Boolean
            get() = CoinValue(io.horizontalsystems.bankwallet.entities.CoinValue.Kind.PlatformCoin(platformCoin), value).isMaxValue
        override val abs: TransactionValue
            get() = copy(value = value.abs())
        override val formattedString: String
            get() = "n/a"

    }

    data class RawValue(override val coinUid: String = "", val value: BigInteger) : TransactionValue() {
        override val coin: Coin? = null
        override val coinName: String = ""
        override val coinCode: String = ""
        override val decimalValue: BigDecimal? = null
        override val decimals: Int? = null
        override val zeroValue: Boolean
            get() = value.compareTo(BigInteger.ZERO) == 0
        override val isMaxValue: Boolean = false
        override val abs: TransactionValue
            get() = copy(value = value.abs())
        override val formattedString: String
            get() = "n/a"

    }

    data class TokenValue(override val coinUid: String = "", val tokenName: String, val tokenCode: String, val tokenDecimals: Int, val value: BigDecimal) : TransactionValue() {
        override val coin: Coin? = null
        override val coinName: String
            get() = tokenName
        override val coinCode: String
            get() = tokenCode
        override val decimalValue: BigDecimal = value
        override val decimals: Int = tokenDecimals
        override val zeroValue: Boolean
            get() = value.compareTo(BigDecimal.ZERO) == 0
        override val isMaxValue: Boolean
            get() = value.isMaxValue(tokenDecimals)
        override val abs: TransactionValue
            get() = copy(value = value.abs())
        override val formattedString: String
            get() = "n/a"

    }
}
