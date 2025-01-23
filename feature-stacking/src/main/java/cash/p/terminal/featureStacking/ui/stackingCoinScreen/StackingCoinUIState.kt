package cash.p.terminal.featureStacking.ui.stackingCoinScreen

import cash.p.terminal.featureStacking.ui.entities.PayoutViewItem
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.wallet.Token
import java.math.BigDecimal

internal data class StackingCoinUIState(
    val stackingType: StackingType = StackingType.PCASH,
    val minStackingAmount: BigDecimal = BigDecimal.ZERO,
    val balance: BigDecimal = BigDecimal.ZERO,
    val balanceStr: String = "",
    val receiveAddress: String? = null,
    val secondaryAmount: String? = null,
    val token: Token? = null,
    val totalIncomeStr: String = "",
    val totalIncomeSecondary: String? = null,
    val unpaidStr: String? = null,
    val unpaidSecondary: String? = null,
    val payoutItems: Map<String, List<PayoutViewItem>> = emptyMap(),
    val loading: Boolean = true,
    val balanceHidden: Boolean = true
) {
    fun isWaitingForStacking(): Boolean =
        balance < minStackingAmount || unpaidStr == null || unpaidStr == ""
}
