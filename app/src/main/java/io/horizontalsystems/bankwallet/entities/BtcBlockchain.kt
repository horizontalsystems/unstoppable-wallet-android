package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

@Parcelize
enum class BtcBlockchain(val raw: String): Parcelable {
    Bitcoin("bitcoin"),
    BitcoinCash("bitcoinCash"),
    Litecoin("litecoin"),
    Dash("dash");

    fun supports(blockchainType: BlockchainType): Boolean = when {
        this == Bitcoin && blockchainType == BlockchainType.Bitcoin -> true
        this == BitcoinCash && blockchainType == BlockchainType.BitcoinCash -> true
        this == Litecoin && blockchainType == BlockchainType.Litecoin -> true
        this == Dash && blockchainType == BlockchainType.Dash -> true
        else -> false
    }

    val title: String
        get() = when (this) {
            Bitcoin -> "Bitcoin"
            BitcoinCash -> "Bitcoin Cash"
            Litecoin -> "Litecoin"
            Dash -> "Dash"
        }

    val icon24: Int
        get() = when (this) {
            Bitcoin -> R.drawable.logo_bitcoin_24
            BitcoinCash -> R.drawable.logo_bitcoincash_24
            Litecoin -> R.drawable.logo_litecoin_24
            Dash -> R.drawable.logo_dash_24
        }
}
