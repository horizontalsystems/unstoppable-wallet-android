package io.horizontalsystems.core.modules.send

import io.horizontalsystems.core.core.HSCaution
import io.horizontalsystems.core.entities.Address
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val address: Address,
)
