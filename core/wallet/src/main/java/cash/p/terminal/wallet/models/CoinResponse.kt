package cash.p.terminal.wallet.models

data class CoinResponse(
    val uid: String,
    val name: String,
    val code: String,
    val market_cap_rank: Int?,
    val coingecko_id: String?,
    val image: String?,
    val priority: Int?
)
