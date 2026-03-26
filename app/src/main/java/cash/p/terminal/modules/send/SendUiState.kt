package cash.p.terminal.modules.send

import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.entities.Address
import java.math.BigDecimal

data class SendUiState(
    val availableBalance: BigDecimal,
    val amountCaution: HSCaution?,
    val addressError: Throwable?,
    val canBeSend: Boolean,
    val showAddressInput: Boolean,
    val address: Address?,
    val cautions: List<CautionViewItem> = emptyList(),
    val fee: BigDecimal? = null,
    val feeLoading: Boolean = false,
    val isPoisonAddress: Boolean = false,
    val riskAccepted: Boolean = false,
)
