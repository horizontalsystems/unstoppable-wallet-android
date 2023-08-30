package io.horizontalsystems.bankwallet.modules.receive.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet

object ReceiveAddressModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveAddressViewModel(wallet, App.adapterManager) as T
        }
    }

    sealed class DescriptionItem {
        class Value(val title: String, val value: String) : DescriptionItem()
        class ValueInfo(val title: String, val value: String, val infoTitle: String, val infoText: String) : DescriptionItem()
    }

    data class PopupWarningItem(
        val title: String,
        val description: String,
    )

    data class UiState(
        val viewState: ViewState,
        val coinCode: String,
        val address: String,
        val qrDescription: String,
        val descriptionItems: List<DescriptionItem>,
        val warning: String?,
        val popupWarningItem: PopupWarningItem?
    )

    class NoReceiverAdapter : Error("No Receiver Adapter")
    class NoWalletData : Error("No Wallet Data")

}
