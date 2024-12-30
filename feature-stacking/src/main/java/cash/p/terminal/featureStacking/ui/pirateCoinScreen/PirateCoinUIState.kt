package cash.p.terminal.featureStacking.ui.pirateCoinScreen

import cash.p.terminal.network.domain.enity.PayoutType
import cash.p.terminal.featureStacking.ui.entities.PayoutViewItem
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

internal data class PirateCoinUIState(
    val balance: BigDecimal = BigDecimal.ZERO,
    val receiveAddress: String? = null,
    val secondaryAmount: String? = null,
    val token: Token? = null,
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalIncomeSecondary: String? = null,
    val unpaid: BigDecimal? = null,
    val unpaidSecondary: String? = null,
    val payoutItems: Map<String, List<PayoutViewItem>> = emptyMap()/*mapOf(
        "DECEMBER 15, 2023" to listOf(
            PayoutViewItem(
                id = 1,
                time = "10:00",
                payoutType = PayoutType.INCOME,
                amount = 123.45.toBigDecimal(),
                amountSecondary = "$123.4"
            ),
            PayoutViewItem(
                id = 2,
                time = "09:55",
                payoutType = PayoutType.INCOME,
                amount = 123.45.toBigDecimal(),
                amountSecondary = "$123.4"
            )
        ),
        "DECEMBER 10, 2023" to listOf(
            PayoutViewItem(
                id = 3,
                time = "09:55",
                payoutType = PayoutType.INCOME,
                amount = 123.45.toBigDecimal(),
                amountSecondary = "$123.4"
            ),
            PayoutViewItem(
                id = 4,
                time = "06:55",
                payoutType = PayoutType.WITHDRAWAL,
                amount = 123.45.toBigDecimal(),
                amountSecondary = "$123.4"
            )
        )
    )*/
//    val payoutItems: Map<String, List<PayoutViewItem>>? = null
)
