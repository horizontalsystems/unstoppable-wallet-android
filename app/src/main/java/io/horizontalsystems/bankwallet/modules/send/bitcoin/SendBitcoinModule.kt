package io.horizontalsystems.bankwallet.modules.send.bitcoin

import io.horizontalsystems.marketkit.models.BlockchainType

object SendBitcoinModule {

    data class UtxoData(
        val type: UtxoType? = null,
        val value: String = "0 / 0",
    )

    enum class UtxoType {
        Auto,
        Manual
    }

    val BlockchainType.rbfSupported: Boolean
        get() = when (this) {
            BlockchainType.Bitcoin,
            BlockchainType.Litecoin -> true
            else -> false
        }
}
