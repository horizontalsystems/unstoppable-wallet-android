package io.horizontalsystems.core.modules.send.tron

import io.horizontalsystems.core.core.HSCaution
import io.horizontalsystems.core.core.ethereum.CautionViewItem
import io.horizontalsystems.core.entities.Address
import io.horizontalsystems.core.entities.ViewState
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val proceedEnabled: Boolean,
    val sendEnabled: Boolean,
    val feeViewState: ViewState,
    val cautions: List<CautionViewItem>,
    val showAddressInput: Boolean,
    val address: Address,
)
