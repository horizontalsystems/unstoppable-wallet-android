package io.horizontalsystems.bankwallet.modules.send.tron

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.ViewState
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val proceedEnabled: Boolean,
    val sendEnabled: Boolean,
    val feeViewState: ViewState,
    val cautions: List<HSCaution>
)
