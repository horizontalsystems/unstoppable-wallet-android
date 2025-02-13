package cash.p.terminal.network.pirate.data.entity

import cash.p.terminal.network.data.serializers.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
internal data class PriceChangeDto(
    @SerialName("percentage_1h")
    val percentage1h: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("percentage_24h")
    val percentage24h: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("percentage_7d")
    val percentage7d: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("percentage_30d")
    val percentage30d: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>,
    @SerialName("percentage_1y")
    val percentage1y: Map<String, @Serializable(with = BigDecimalSerializer::class) BigDecimal>
)
