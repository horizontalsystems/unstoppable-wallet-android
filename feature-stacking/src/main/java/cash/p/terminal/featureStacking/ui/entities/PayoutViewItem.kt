package cash.p.terminal.featureStacking.ui.entities

import cash.p.terminal.network.domain.enity.PayoutType
import java.math.BigDecimal

internal data class PayoutViewItem(
    val id: Long,
    val date: String,
    val time: String,
    val payoutType: PayoutType,
    val amount: BigDecimal,
    val amountSecondary: String
)