package cash.p.terminal.network.pirate.domain.enity

data class ChangeNowAssociatedCoin(
    val ticker: String,
    val name: String,
    val blockchain: String,
    val coinId: String,
)