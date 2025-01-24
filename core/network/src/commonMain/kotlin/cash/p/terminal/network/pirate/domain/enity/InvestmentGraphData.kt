package cash.p.terminal.network.pirate.domain.enity

data class InvestmentGraphData(
    val points: List<PricePoint>
)

data class PricePoint(
    val value: Double,
    val balance: Double,
    val from: Long,
    val to: Long,
    val price: Map<String, String>,
    val balancePrice: Map<String, String>
)