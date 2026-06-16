package cash.p.terminal.network.quickex.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal class TransactionQuickexStatusDto(
    val orderId: String,
    val createdAt: String,
    val orderEvents: List<OrderEventDto>,
    val completed: Boolean,
    val withdrawals: List<WithdrawalDto>? = null
)

@Serializable
internal class OrderEventDto(
    val kind: String,
    val createdAt: String
)

@Serializable
internal class WithdrawalDto(
    val amount: String,
    val txId: String? = null,
    val createdAt: String? = null
)
