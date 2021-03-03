package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.coinkit.models.CoinType
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class CommunicationMode(val value: String) : Parcelable {
    Infura("Infura"),
    Incubed("Incubed"),
    BinanceDex("BinanceDex");

    val title: String
        get() = when (this) {
            Infura -> "Infura"
            Incubed -> "Incubed"
            BinanceDex -> "Binance Dex"
        }
}

class EthereumRpcMode(val coinType: CoinType,
                      val communicationMode: CommunicationMode)
