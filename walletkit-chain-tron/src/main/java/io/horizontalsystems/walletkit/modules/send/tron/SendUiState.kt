package io.horizontalsystems.walletkit.modules.send.tron

import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.ViewState
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
