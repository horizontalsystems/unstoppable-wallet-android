package io.horizontalsystems.bankwallet.modules.balance.token

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.balance.AttentionIcon
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import java.math.BigDecimal

object TokenBalanceModule {

    data class TokenBalanceUiState(
        val title: String,
        val balanceViewItem: BalanceViewItem?,
        val transactions: Map<String, List<TransactionViewItem>>?,
        val receiveAddress: String?,
        val error: TokenBalanceError? = null,
        val failedErrorMessage: String?,
        val warningMessage: String?,
        val alertUnshieldedBalance: BigDecimal?,
        val attentionIcon: AttentionIcon?,
        val showTronNotActiveAlert: Boolean,
    )

    data class TokenBalanceError(
        val message: String,
        val errorTitle: String? = null,
        val icon: Int = R.drawable.warning_filled_24,
        val showRetryButton: Boolean = false,
        val showChangeSourceButton: Boolean = false,
    )

}
