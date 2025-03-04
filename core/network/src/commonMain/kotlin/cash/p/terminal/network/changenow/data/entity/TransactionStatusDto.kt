package cash.p.terminal.network.changenow.data.entity

import kotlinx.serialization.Serializable

@Serializable
class TransactionStatusDto(
    val status: String,
    val payinAddress: String,
    val payoutAddress: String,
    val fromCurrency: String,
    val toCurrency: String,
    val id: String,
    val updatedAt: String
)