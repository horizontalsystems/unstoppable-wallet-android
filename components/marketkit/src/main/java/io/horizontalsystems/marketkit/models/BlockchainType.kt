package io.horizontalsystems.marketkit.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class BlockchainType : Parcelable {
    @Parcelize
    object Bitcoin : BlockchainType()

    @Parcelize
    object BitcoinCash : BlockchainType()

    @Parcelize
    object ECash : BlockchainType()

    @Parcelize
    object Litecoin : BlockchainType()

    @Parcelize
    object Dash : BlockchainType()

    @Parcelize
    object Zcash : BlockchainType()

    @Parcelize
    object Ethereum : BlockchainType()

    @Parcelize
    object BinanceSmartChain : BlockchainType()

    @Parcelize
    object BinanceChain : BlockchainType()

    @Parcelize
    object Polygon : BlockchainType()

    @Parcelize
    object Avalanche : BlockchainType()

    @Parcelize
    object Optimism : BlockchainType()

    @Parcelize
    object ArbitrumOne : BlockchainType()

    @Parcelize
    object Solana : BlockchainType()

    @Parcelize
    object Gnosis : BlockchainType()

    @Parcelize
    object Fantom : BlockchainType()

    @Parcelize
    object Tron: BlockchainType()

    @Parcelize
    class Unsupported(val _uid: String) : BlockchainType()

    val uid: String
        get() = when (this) {
            is Bitcoin -> "bitcoin"
            is BitcoinCash -> "bitcoin-cash"
            is ECash -> "ecash"
            is Litecoin -> "litecoin"
            is Dash -> "dash"
            is Zcash -> "zcash"
            is Ethereum -> "ethereum"
            is BinanceSmartChain -> "binance-smart-chain"
            is BinanceChain -> "binancecoin"
            is Polygon -> "polygon-pos"
            is Avalanche -> "avalanche"
            is Optimism -> "optimistic-ethereum"
            is ArbitrumOne -> "arbitrum-one"
            is Solana -> "solana"
            is Gnosis -> "gnosis"
            is Fantom -> "fantom"
            is Tron -> "tron"
            is Unsupported -> this._uid
        }

    override fun equals(other: Any?): Boolean {
        return other is BlockchainType && other.uid == uid
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString() = when (this) {
        Bitcoin -> "bitcoin"
        BitcoinCash -> "bitcoinCash"
        ECash -> "ecash"
        Litecoin -> "litecoin"
        Dash -> "dash"
        Zcash -> "zcash"
        Ethereum -> "ethereum"
        BinanceSmartChain -> "binanceSmartChain"
        Polygon -> "polygon"
        Avalanche -> "avalanche"
        ArbitrumOne -> "arbitrumOne"
        BinanceChain -> "binanceChain"
        Optimism -> "optimism"
        Solana -> "solana"
        Gnosis -> "gnosis"
        Fantom -> "fantom"
        Tron -> "tron"
        is Unsupported -> "unsupported|$uid"
    }

    companion object {

        fun fromUid(uid: String): BlockchainType =
            when (uid) {
                "bitcoin" -> Bitcoin
                "bitcoin-cash" -> BitcoinCash
                "ecash" -> ECash
                "litecoin" -> Litecoin
                "dash" -> Dash
                "zcash" -> Zcash
                "ethereum" -> Ethereum
                "binance-smart-chain" -> BinanceSmartChain
                "binancecoin" -> BinanceChain
                "polygon-pos" -> Polygon
                "avalanche" -> Avalanche
                "optimistic-ethereum" -> Optimism
                "arbitrum-one" -> ArbitrumOne
                "solana" -> Solana
                "gnosis" -> Gnosis
                "fantom" -> Fantom
                "tron" -> Tron
                else -> Unsupported(uid)
            }

    }

}
