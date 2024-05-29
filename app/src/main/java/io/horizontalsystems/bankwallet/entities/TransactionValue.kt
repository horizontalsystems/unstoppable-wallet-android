package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.BigInteger

sealed class TransactionValue {
    abstract val fullName: String
    abstract val coinUid: String
    abstract val coinCode: String
    abstract val coin: Coin?
    abstract val badge: String?
    abstract val coinIconUrl: String?
    abstract val alternativeCoinIconUrl: String?
    abstract val coinIconPlaceholder: Int?
    abstract val decimalValue: BigDecimal?
    abstract val decimals: Int?
    abstract val zeroValue: Boolean
    abstract val isMaxValue: Boolean
    abstract val abs: TransactionValue
    abstract val formattedString: String

    open val nftUid: NftUid? = null

    data class CoinValue(val token: Token, val value: BigDecimal) : TransactionValue() {
        override val coin: Coin = token.coin
        override val badge: String? = token.badge
        override val coinIconUrl = token.coin.imageUrl
        override val alternativeCoinIconUrl = token.coin.alternativeImageUrl
        override val coinIconPlaceholder = token.fullCoin.iconPlaceholder
        override val coinUid: String = coin.uid
        override val fullName: String = coin.name
        override val coinCode: String = coin.code
        override val decimalValue: BigDecimal = value
        override val decimals: Int = token.decimals
        override val zeroValue: Boolean
            get() = value.compareTo(BigDecimal.ZERO) == 0
        override val isMaxValue: Boolean
            get() = value.isMaxValue(token.decimals)
        override val abs: TransactionValue
            get() = copy(value = value.abs())
        override val formattedString: String
            get() = "n/a"

    }

    data class RawValue(val value: BigInteger) : TransactionValue() {
        override val coinUid: String = ""
        override val coin: Coin? = null
        override val badge: String? = null
        override val coinIconUrl = null
        override val alternativeCoinIconUrl = null
        override val coinIconPlaceholder = null
        override val fullName: String = ""
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

    data class TokenValue(
        val tokenName: String,
        val tokenCode: String,
        val tokenDecimals: Int,
        val value: BigDecimal,
        override val coinIconPlaceholder: Int? = null
    ) : TransactionValue() {
        override val coinUid: String = ""
        override val coin: Coin? = null
        override val badge: String? = null
        override val coinIconUrl = null
        override val alternativeCoinIconUrl = null
        override val fullName: String
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

    data class NftValue(
        override val nftUid: NftUid,
        val value: BigDecimal,
        val tokenName: String?,
        val tokenSymbol: String?
    ) : TransactionValue() {
        override val coinUid: String = ""
        override val coin: Coin? = null
        override val badge: String? = null
        override val coinIconUrl = null
        override val alternativeCoinIconUrl = null
        override val coinIconPlaceholder: Int? = null
        override val fullName: String
            get() = "${tokenName ?: ""} #${nftUid.tokenId}"
        override val coinCode: String
            get() = tokenSymbol ?: "NFT"
        override val decimalValue: BigDecimal = value
        override val decimals: Int? = null
        override val zeroValue: Boolean
            get() = value.compareTo(BigDecimal.ZERO) == 0
        override val isMaxValue = false
        override val abs: TransactionValue
            get() = copy(value = value.abs())
        override val formattedString: String
            get() = "n/a"

    }
}
