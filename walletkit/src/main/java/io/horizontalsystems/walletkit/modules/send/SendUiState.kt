package io.horizontalsystems.walletkit.modules.send

import io.horizontalsystems.walletkit.core.HSCaution
import io.horizontalsystems.walletkit.entities.Address
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val address: Address,
)
