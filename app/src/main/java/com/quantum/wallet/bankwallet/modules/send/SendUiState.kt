package com.quantum.wallet.bankwallet.modules.send

import com.quantum.wallet.bankwallet.core.HSCaution
import com.quantum.wallet.bankwallet.entities.Address
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val address: Address,
)
