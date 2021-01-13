package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

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
    class Erc20(val address: String, val fee: BigDecimal = BigDecimal.ZERO, val minimumRequiredBalance: BigDecimal = BigDecimal.ZERO, val minimumSendAmount: BigDecimal = BigDecimal.ZERO) : CoinType()

    @Parcelize
    class Binance(val symbol: String) : CoinType()

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
            is Binance -> if (symbol != "BNB") "BEP2" else null
            else -> null
        }

    val predefinedAccountType: PredefinedAccountType
        get() = when (this) {
            Bitcoin, Litecoin, BitcoinCash, Dash, Ethereum, is Erc20 -> PredefinedAccountType.Standard
            is Binance -> PredefinedAccountType.Binance
            Zcash -> PredefinedAccountType.Zcash
        }

    val swappable: Boolean
        get() = this is Ethereum || this is Erc20

    fun canSupport(accountType: AccountType) = when (this) {
        Bitcoin, Litecoin, BitcoinCash, Dash, Ethereum, is Erc20 -> {
            accountType is AccountType.Mnemonic && accountType.words.size == 12 && accountType.salt == null
        }
        is Binance -> {
            accountType is AccountType.Mnemonic && accountType.words.size == 24 && accountType.salt == null
        }
        Zcash -> {
            accountType is AccountType.Zcash && accountType.words.size == 24
        }
    }

}
