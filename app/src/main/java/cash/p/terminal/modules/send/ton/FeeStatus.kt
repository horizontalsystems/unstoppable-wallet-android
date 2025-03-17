package cash.p.terminal.modules.send.ton

import java.math.BigDecimal

sealed interface FeeStatus {
    object NoEnoughBalance : FeeStatus
    data class Success(val fee: BigDecimal) : FeeStatus
}
