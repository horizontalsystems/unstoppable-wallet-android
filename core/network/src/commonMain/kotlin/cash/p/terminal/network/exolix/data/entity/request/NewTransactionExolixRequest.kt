package cash.p.terminal.network.exolix.data.entity.request

import kotlinx.serialization.Serializable

@Serializable
data class NewTransactionExolixRequest(
    val coinFrom: String,
    val networkFrom: String,
    val coinTo: String,
    val networkTo: String,
    val amount: String,
    val withdrawalAddress: String,
    val withdrawalExtraId: String? = null,
    val rateType: String,
    val refundAddress: String,
    val refundExtraId: String? = null,
)
