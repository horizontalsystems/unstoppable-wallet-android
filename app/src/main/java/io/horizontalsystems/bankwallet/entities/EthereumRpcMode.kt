package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class CommunicationMode(val value: String) : Parcelable {
    Infura("Infura"),
    Incubed("Incubed"),
    Greymass("GreyMass"),
    BinanceDex("BinanceDex");

    val title: String
        get() = when (this) {
            Infura -> "Infura"
            Incubed -> "Incubed"
            Greymass -> "Greymass"
            BinanceDex -> "Binance Dex"
        }
}

class EthereumRpcMode(val coinType: CoinType,
                      val communicationMode: CommunicationMode)
