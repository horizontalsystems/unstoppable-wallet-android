package cash.p.terminal.modules.send.tron

import cash.p.terminal.core.HSCaution
import io.horizontalsystems.core.entities.ViewState
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val proceedEnabled: Boolean,
    val sendEnabled: Boolean,
    val feeViewState: ViewState,
    val cautions: List<HSCaution>,
    val showAddressInput: Boolean,
)
