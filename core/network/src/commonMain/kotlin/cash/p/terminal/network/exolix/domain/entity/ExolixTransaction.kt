package cash.p.terminal.network.exolix.domain.entity

import java.math.BigDecimal

data class ExolixTransaction(
    val id: String,
    val amount: BigDecimal,
    val amountTo: BigDecimal,
    val createdAt: String?,
    val updatedAt: String?,
    val depositAddress: String,
    val depositExtraId: String?,
    val withdrawalAddress: String,
    val withdrawalExtraId: String?,
    val status: String,
)
