package com.quantum.wallet.bankwallet.modules.send.tron

import com.quantum.wallet.bankwallet.core.HSCaution
import com.quantum.wallet.bankwallet.core.ethereum.CautionViewItem
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.ViewState
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
