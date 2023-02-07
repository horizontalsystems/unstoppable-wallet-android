package cash.p.terminal.modules.send

import cash.p.terminal.core.HSCaution
import java.math.BigDecimal


data class SendUiState(
        val availableBalance: BigDecimal,
        val amountCaution: HSCaution?,
        val addressError: Throwable?,
        val canBeSend: Boolean
)
