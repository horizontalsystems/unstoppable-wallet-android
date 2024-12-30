package cash.p.terminal.network.domain.enity

data class InvestmentData(
    val id: Int,
    val chain: String,
    val source: String,
    val address: String,
    val balance: String,
    val unrealizedValue: String,
    val mint: String,
    val balancePrice: Map<String, String>,
    val unrealizedValuePrice: Map<String, String>,
    val mintPrice: Map<String, String>
)