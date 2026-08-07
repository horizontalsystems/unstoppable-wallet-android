package io.horizontalsystems.walletkit.modules.send.bitcoin


data class UtxoData(
        val type: UtxoType? = null,
        val value: String = "0 / 0",
)

enum class UtxoType {
        Auto,
        Manual
}
