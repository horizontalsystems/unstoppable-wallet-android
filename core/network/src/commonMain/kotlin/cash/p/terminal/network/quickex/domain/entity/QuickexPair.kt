package cash.p.terminal.network.quickex.domain.entity

data class QuickexInstrument(
    val currencyTitle: String,
    val networkTitle: String,
    val currencyFriendlyTitle: String,
    val slug: String,
    val precisionDecimals: Int,
    val requiresMemo: Boolean,
    val bestChangeName: String,
    val contractAddress: String
)
