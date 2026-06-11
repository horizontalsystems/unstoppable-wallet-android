package cash.p.terminal.network.exolix.data.entity

import cash.p.terminal.network.data.serializers.FlexibleBigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
internal data class ExolixRateDto(
    @Serializable(with = FlexibleBigDecimalSerializer::class)
    val fromAmount: BigDecimal,
    @Serializable(with = FlexibleBigDecimalSerializer::class)
    val toAmount: BigDecimal,
    @Serializable(with = FlexibleBigDecimalSerializer::class)
    val rate: BigDecimal,
    val message: String? = null,
    @Serializable(with = FlexibleBigDecimalSerializer::class)
    val minAmount: BigDecimal,
    @Serializable(with = FlexibleBigDecimalSerializer::class)
    val withdrawMin: BigDecimal,
    @Serializable(with = FlexibleBigDecimalSerializer::class)
    val maxAmount: BigDecimal? = null,
)
