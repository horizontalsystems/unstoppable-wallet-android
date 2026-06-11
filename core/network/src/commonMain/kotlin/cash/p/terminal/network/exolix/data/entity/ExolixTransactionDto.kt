package cash.p.terminal.network.exolix.data.entity

import cash.p.terminal.network.data.serializers.FlexibleBigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
internal data class ExolixTransactionDto(
    val id: String,
    @Serializable(with = FlexibleBigDecimalSerializer::class)
    val amount: BigDecimal,
    @Serializable(with = FlexibleBigDecimalSerializer::class)
    val amountTo: BigDecimal,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val depositAddress: String,
    val depositExtraId: String? = null,
    val withdrawalAddress: String,
    val withdrawalExtraId: String? = null,
    val status: String,
)
