package cash.p.terminal.network.pirate.domain.enity

import kotlinx.serialization.Serializable

@Serializable
data class ChangeNowAssociatedCoin(
    val ticker: String,
    val name: String,
    val blockchain: String,
    val coinId: String,
)