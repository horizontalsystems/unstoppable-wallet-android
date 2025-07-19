package cash.p.terminal.modules.send.tron

import cash.p.terminal.core.HSCaution
import cash.p.terminal.entities.Address
import cash.p.terminal.ui_compose.entities.ViewState
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
    val address: Address,
)
