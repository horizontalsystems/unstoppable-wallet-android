package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.UsedAddress
import io.horizontalsystems.bankwallet.entities.ViewState
import java.math.BigDecimal

object ReceiveModule {

    sealed class AdditionalData {
        class Amount(val value: String) : AdditionalData()
        class Memo(val value: String) : AdditionalData()
    }

    abstract class AbstractUiState {
        abstract val viewState: ViewState
        abstract val alertText: AlertText?
        abstract val uri: String
        abstract val address: String
        abstract val mainNet: Boolean
        abstract val blockchainName: String?
        abstract val addressType: String?
        abstract val addressFormat: String?
        abstract val watchAccount: Boolean
        abstract val amount: BigDecimal?
        abstract val amountString: String?
    }

    data class UiState(
        override val viewState: ViewState,
        override val address: String,
        override val mainNet: Boolean,
        val usedAddresses: List<UsedAddress>,
        val usedChangeAddresses: List<UsedAddress>,
        override val uri: String,
        override val blockchainName: String?,
        override val addressType: String?,
        override val addressFormat: String?,
        override val watchAccount: Boolean,
        override val amount: BigDecimal?,
        override val amountString: String?,
        override val alertText: AlertText?,
    ) : AbstractUiState()

    sealed class AlertText {
        class Critical(val content: String) : AlertText()
    }

}
