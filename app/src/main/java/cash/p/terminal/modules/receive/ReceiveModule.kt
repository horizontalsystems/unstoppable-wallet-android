package cash.p.terminal.modules.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.wallet.entities.UsedAddress
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.modules.receive.viewmodels.ReceiveAddressViewModel
import java.math.BigDecimal

object ReceiveModule {

    class Factory(private val wallet: cash.p.terminal.wallet.Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveAddressViewModel(wallet, App.adapterManager) as T
        }
    }

    sealed class AdditionalData {
        class Amount(val value: String) : AdditionalData()
        class Memo(val value: String) : AdditionalData()
        object AccountNotActive : AdditionalData()
    }

    data class UiState(
        val viewState: ViewState,
        val address: String,
        val usedAddresses: List<UsedAddress>,
        val usedChangeAddresses: List<UsedAddress>,
        val uri: String,
        val networkName: String,
        val watchAccount: Boolean,
        val additionalItems: List<AdditionalData>,
        val amount: BigDecimal?,
        val alertText: AlertText?,
    )

    sealed class AlertText {
        class Normal(val content: String) : AlertText()
        class Critical(val content: String) : AlertText()
    }

}
