package cash.p.terminal.network.changenow.data.entity.request

import kotlinx.serialization.Serializable

@Serializable
class NewTransactionRequest(
    val from: String,
    val to: String,
    val address: String,
    val amount: String,
    val refundAddress: String,
    val payoutExtraId: String = "",
    val refundExtraId: String = ""
)