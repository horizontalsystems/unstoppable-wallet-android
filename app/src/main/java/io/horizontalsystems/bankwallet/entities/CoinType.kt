package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

sealed class CoinType : Parcelable {
    @Parcelize
    object Bitcoin : CoinType()

    @Parcelize
    object Litecoin : CoinType()

    @Parcelize
    object BitcoinCash : CoinType()

    @Parcelize
    object Dash : CoinType()

    @Parcelize
    object Ethereum : CoinType()

    @Parcelize
    class Erc20(val address: String, val fee: BigDecimal = BigDecimal.ZERO, val minimumRequiredBalance: BigDecimal = BigDecimal.ZERO, val minimumSendAmount: BigDecimal = BigDecimal.ZERO) : CoinType() {
        override fun equals(other: Any?): Boolean {
            if (other is Erc20) {
                return other.address.equals(address, ignoreCase = true)
            }

            return super.equals(other)
        }

        override fun hashCode(): Int {
            return address.toLowerCase(Locale.ENGLISH).hashCode()
        }
    }

    @Parcelize
    object BinanceSmartChain : CoinType()

    @Parcelize
    class Bep20(val address: String) : CoinType() {
        override fun equals(other: Any?): Boolean {
            return other is Bep20 && other.address.equals(address, ignoreCase = true)
        }

        override fun hashCode(): Int {
            return address.toLowerCase(Locale.ENGLISH).hashCode()
        }
    }

    @Parcelize
    class Binance(val symbol: String) : CoinType() {
        override fun equals(other: Any?): Boolean {
            if (other is Binance) {
                return other.symbol.equals(symbol, ignoreCase = true)
            }

            return super.equals(other)
        }

        override fun hashCode(): Int {
            return symbol.hashCode()
        }
    }

    @Parcelize
    object Zcash : CoinType()

    val title: String
        get() = when (this) {
            is Bitcoin -> "Bitcoin"
            is Litecoin -> "Litecoin"
            is BitcoinCash -> "BitcoinCash"
            else -> ""
        }

    val label: String?
        get() = when (this) {
            is Erc20 -> "ERC20"
            is Bep20 -> "BEP20"
            is Binance -> "BEP2"
            else -> null
        }

    val predefinedAccountType: PredefinedAccountType
        get() = when (this) {
            Bitcoin, Litecoin, BitcoinCash, Dash, Ethereum, is Erc20 -> PredefinedAccountType.Standard
            BinanceSmartChain, is Bep20, is Binance -> PredefinedAccountType.Binance
            Zcash -> PredefinedAccountType.Zcash
        }

    val swappable: Boolean
        get() = this is Ethereum || this is Erc20

    fun canSupport(accountType: AccountType) = when (this) {
        Bitcoin, Litecoin, BitcoinCash, Dash, Ethereum, is Erc20 -> {
            accountType is AccountType.Mnemonic && accountType.words.size == 12 && accountType.salt == null
        }
        BinanceSmartChain, is Bep20, is Binance -> {
            accountType is AccountType.Mnemonic && accountType.words.size == 24 && accountType.salt == null
        }
        Zcash -> {
            accountType is AccountType.Zcash && accountType.words.size == 24
        }
    }

    fun serialize(): String {
        return when (this) {
            Bitcoin -> bitcoin
            Litecoin -> litecoin
            BitcoinCash -> bitcoinCash
            Dash -> dash
            Ethereum -> ethereum
            is Erc20 -> arrayOf(erc20, address).joinToString(separator)
            is Binance -> arrayOf(binance, symbol).joinToString(separator)
            BinanceSmartChain -> binanceSmartChain
            is Bep20 -> arrayOf(bep20, address).joinToString(separator)
            Zcash -> zcash
        }
    }

    companion object {
        const val bitcoin = "bitcoin"
        const val litecoin = "litecoin"
        const val bitcoinCash = "bitcoincash"
        const val dash = "dash"
        const val ethereum = "ethereum"
        const val erc20 = "erc20"
        const val binance = "binance"
        const val binanceSmartChain = "binanceSmartChain"
        const val bep20 = "bep20"
        const val zcash = "zcash"

        private const val separator = ":"

        fun deserialize(value: String): CoinType? {
            val parts = value.split(separator)

            return when (parts.firstOrNull()) {
                bitcoin -> Bitcoin
                litecoin -> Litecoin
                bitcoinCash -> BitcoinCash
                dash -> Dash
                ethereum -> Ethereum
                erc20 -> Erc20(parts[1])
                binance -> Binance(parts[1])
                binanceSmartChain -> BinanceSmartChain
                bep20 -> Bep20(parts[1])
                zcash -> Zcash
                else -> null
            }
        }
    }

}
