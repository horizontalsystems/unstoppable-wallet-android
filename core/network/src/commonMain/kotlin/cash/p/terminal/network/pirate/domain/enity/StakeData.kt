package cash.p.terminal.network.pirate.domain.enity

data class StakeData(
    val stakes: List<Stake>
)

data class Stake(
    val id: Long,
    val type: PayoutType,
    val balance: Double,
    val amount: Double,
    val createdAt: Long,
    val balancePrice: Map<String, String>,
    val amountPrice: Map<String, String>
)