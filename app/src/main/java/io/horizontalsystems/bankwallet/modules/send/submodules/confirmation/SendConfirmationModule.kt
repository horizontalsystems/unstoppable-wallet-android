package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendPresenter
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

object SendConfirmationModule {

    //const val ConfirmationInfoKey = "confirmation_info_key"

    interface View{
        fun loadPrimaryItems(primaryItemData: PrimaryItemData)
        fun loadSecondaryItems(secondaryItemData: SecondaryItemData)
        fun showCopied()
        fun loadSendButton()
        fun setSendButtonState(state: SendButtonState)
    }

    interface ViewDelegate {
        fun onViewDidLoad()
        fun onReceiverClick()
        fun onSendError()
    }

    interface Interactor {
        fun copyToClipboard(coinAddress: String)
    }

    interface InteractorDelegate {
        fun didCopyToClipboard()
    }

    enum class SendButtonState {
        ACTIVE, SENDING
    }

    data class PrimaryItemData(
            val primaryName: String,
            val primaryAmount: String,
            val secondaryName: String?,
            val secondaryAmount: String?,
            val receiver: String,
            val memo: String?)

    data class SecondaryItemData(
            val feeAmount: String?,
            val estimatedTime: Long?)


    class Factory() : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendConfirmationView()
            val interactor = SendConfirmationInteractor(TextHelper)
            val presenter = SendConfirmationPresenter(view, interactor )

            interactor.delegate = presenter

            return presenter as T
        }
    }

}
