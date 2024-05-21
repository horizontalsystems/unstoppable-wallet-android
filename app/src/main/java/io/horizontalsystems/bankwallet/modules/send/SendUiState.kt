package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.Currency
import java.math.BigDecimal


data class SendUiState(
        val availableBalance: BigDecimal,
        val amountCaution: HSCaution?,
        val addressError: Throwable?,
        val canBeSend: Boolean,
        val showAddressInput: Boolean,
        val currency: Currency,
        val amount: BigDecimal?,
        val fiatAmountInputEnabled: Boolean,
        val fiatAmount: BigDecimal?,
)
