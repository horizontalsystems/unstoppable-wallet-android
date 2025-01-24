package cash.p.terminal.network.changenow.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal class NewTransactionResponseDto(
    val payinAddress: String,
    val payoutAddress: String,
    val fromCurrency: String,
    val toCurrency: String,
    val id: String,
    val amount: String,
    val refundAddress: String? = null,
    val refundExtraId: String? = null,
    val payoutExtraId: String? = null,
)