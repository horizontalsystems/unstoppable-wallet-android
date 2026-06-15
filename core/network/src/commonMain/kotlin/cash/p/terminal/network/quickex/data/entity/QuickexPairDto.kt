package cash.p.terminal.network.quickex.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class QuickexInstrumentDto(
    val currencyTitle: String,
    val networkTitle: String,
    val currencyFriendlyTitle: String,
    val slug: String,
    val precisionDecimals: Int,
    val requiresMemo: Boolean,
    val instrumentType: String,
    val bestChangeName: String,
    val contractAddress: String,
)
