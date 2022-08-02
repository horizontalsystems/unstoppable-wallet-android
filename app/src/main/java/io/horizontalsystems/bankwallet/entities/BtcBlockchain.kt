package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.parcelize.Parcelize

@Parcelize
enum class BtcBlockchain(val raw: String): Parcelable {
    Bitcoin("bitcoin"),
    BitcoinCash("bitcoinCash"),
    Litecoin("litecoin"),
    Litecoin("tyzen"),
    Dash("dash");

    fun supports(coinType: CoinType): Boolean = when {
        this == Bitcoin && coinType == CoinType.Bitcoin -> true
        this == BitcoinCash && coinType == CoinType.BitcoinCash -> true
        this == Litecoin && coinType == CoinType.Litecoin -> true
        this == Tyzen && coinType == CoinType.Tyzen -> true
        this == Dash && coinType == CoinType.Dash -> true
        else -> false
    }

    val title: String
        get() = when (this) {
            Bitcoin -> "Bitcoin"
            BitcoinCash -> "Bitcoin Cash"
            Litecoin -> "Litecoin"
            Tyzen -> "Tyzen"
            Dash -> "Dash"
        }

    val icon24: Int
        get() = when (this) {
            Bitcoin -> R.drawable.logo_bitcoin_24
            BitcoinCash -> R.drawable.logo_bitcoincash_24
            Litecoin -> R.drawable.logo_litecoin_24
            Tyzen -> R.drawable.logo_tyzen_24
            Dash -> R.drawable.logo_dash_24
        }
}
