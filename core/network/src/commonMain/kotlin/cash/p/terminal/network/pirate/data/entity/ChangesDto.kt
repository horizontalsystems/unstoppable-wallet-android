package cash.p.terminal.network.pirate.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChangesDto(
    val price: PriceChangeDto,
    @SerialName("market_cap")
    val marketCap: MarketCapDto
)
