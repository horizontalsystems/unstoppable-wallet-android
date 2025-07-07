package io.horizontalsystems.core.entities

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed class BlockchainType : Parcelable {
    abstract val uid: String
    open val stringRepresentation: String get() = uid

    @Parcelize
    object Bitcoin : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "bitcoin"
    }

    @Parcelize
    object BitcoinCash : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "bitcoin-cash"
        @IgnoredOnParcel
        override val stringRepresentation = "bitcoinCash"
    }

    @Parcelize
    object ECash : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "ecash"
    }

    @Parcelize
    object Litecoin : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "litecoin"
    }

    @Parcelize
    object Dogecoin : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "dogecoin"
    }

    @Parcelize
    object Dash : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "dash"
    }

    @Parcelize
    object Zcash : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "zcash"
    }

    @Parcelize
    object Ethereum : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "ethereum"
    }

    @Parcelize
    object BinanceSmartChain : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "binance-smart-chain"
        @IgnoredOnParcel
        override val stringRepresentation = "binanceSmartChain"
    }

    @Parcelize
    object Polygon : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "polygon-pos"
        @IgnoredOnParcel
        override val stringRepresentation = "polygon"
    }

    @Parcelize
    object Avalanche : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "avalanche"
    }

    @Parcelize
    object Optimism : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "optimistic-ethereum"
        @IgnoredOnParcel
        override val stringRepresentation = "optimism"
    }

    @Parcelize
    object ArbitrumOne : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "arbitrum-one"
        @IgnoredOnParcel
        override val stringRepresentation = "arbitrumOne"
    }

    @Parcelize
    object Solana : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "solana"
    }

    @Parcelize
    object Gnosis : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "gnosis"
    }

    @Parcelize
    object Fantom : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "fantom"
    }

    @Parcelize
    object Tron : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "tron"
    }

    @Parcelize
    object Ton : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "the-open-network"
    }

    @Parcelize
    object Base : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "base"
    }

    @Parcelize
    object Cosanta : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "cosanta"
    }

    @Parcelize
    object PirateCash : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "piratecash"
    }

    @Parcelize
    object ZkSync : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "zksync"
    }

    @Parcelize
    object Monero : BlockchainType() {
        @IgnoredOnParcel
        override val uid = "monero"
    }

    @Parcelize
    class Unsupported(val _uid: String) : BlockchainType() {
        @IgnoredOnParcel
        override val uid: String get() = _uid
        @IgnoredOnParcel
        override val stringRepresentation: String get() = "unsupported|$uid"
    }

    override fun equals(other: Any?): Boolean {
        return other is BlockchainType && other.uid == uid
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString() = stringRepresentation

    companion object {
        fun fromUid(uid: String): BlockchainType = when (uid) {
            "bitcoin" -> Bitcoin
            "bitcoin-cash" -> BitcoinCash
            "ecash" -> ECash
            "litecoin" -> Litecoin
            "dogecoin" -> Dogecoin
            "dash" -> Dash
            "zcash" -> Zcash
            "ethereum" -> Ethereum
            "binance-smart-chain" -> BinanceSmartChain
            "polygon-pos" -> Polygon
            "avalanche" -> Avalanche
            "optimistic-ethereum" -> Optimism
            "arbitrum-one" -> ArbitrumOne
            "solana" -> Solana
            "gnosis" -> Gnosis
            "fantom" -> Fantom
            "tron" -> Tron
            "the-open-network" -> Ton
            "base" -> Base
            "cosanta" -> Cosanta
            "piratecash" -> PirateCash
            "zksync" -> ZkSync
            "monero" -> Monero
            else -> Unsupported(uid)
        }
    }
}