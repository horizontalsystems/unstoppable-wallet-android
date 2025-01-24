package cash.p.terminal.network.pirate.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class ChangeNowAssociatedCoinDto(
    val ticker: String?,
    val name: String?,
    val blockchain: String?,
    @SerialName("coin_id")
    val coinId: String?,
)