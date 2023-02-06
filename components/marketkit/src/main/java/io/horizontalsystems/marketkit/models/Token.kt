package io.horizontalsystems.marketkit.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Token(
    val coin: Coin,
    val blockchain: Blockchain,
    val type: TokenType,
    val decimals: Int
) : Parcelable {

    val blockchainType: BlockchainType
        get() = blockchain.type

    val tokenQuery: TokenQuery
        get() = TokenQuery(blockchainType, type)

    val fullCoin: FullCoin
        get() = FullCoin(coin, listOf(this))

    override fun equals(other: Any?): Boolean =
        other is Token && other.coin == coin && other.blockchain == blockchain && other.type == type && other.decimals == decimals

    override fun hashCode(): Int =
        Objects.hash(coin, blockchain, type, decimals)

}
