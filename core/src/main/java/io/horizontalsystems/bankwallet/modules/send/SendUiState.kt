package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.Address
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val address: Address,
)
