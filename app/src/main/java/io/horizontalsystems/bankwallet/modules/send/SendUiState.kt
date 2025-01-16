package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Currency
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val coinAmount: BigDecimal?,
    val fiatAmount: BigDecimal?,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val canBeSendToAddress: Boolean,
    val address: Address?,
    val currency: Currency,
)
