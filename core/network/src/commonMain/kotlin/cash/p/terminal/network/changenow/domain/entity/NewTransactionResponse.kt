package cash.p.terminal.network.changenow.domain.entity

import java.math.BigDecimal

data class NewTransactionResponse(
    val payinAddress: String,
    val payoutAddress: String,
    val payoutExtraId: String?,
    val fromCurrency: String,
    val toCurrency: String,
    val refundAddress: String?,
    val refundExtraId: String?,
    val id: String,
    val amount: BigDecimal
)