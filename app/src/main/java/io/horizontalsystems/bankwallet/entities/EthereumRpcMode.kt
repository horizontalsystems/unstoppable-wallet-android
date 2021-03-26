package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.coinkit.models.CoinType
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class CommunicationMode(val value: String) : Parcelable {
    Infura("Infura"),
    Nariox("Nariox"),
    Incubed("Incubed"),
    BinanceDex("BinanceDex");

    val title: String
        get() = when (this) {
            Infura -> "infura.io"
            Nariox -> "bsc-ws-node.nariox.org"
            Incubed -> "Incubed"
            BinanceDex -> "dex.binance.com"
        }
}

class EthereumRpcMode(val coinType: CoinType,
                      val communicationMode: CommunicationMode)
