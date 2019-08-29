package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

object SendConfirmationModule {

    const val ConfirmationInfoKey = "confirmation_info_key"

    interface IView{
        fun loadPrimaryItems(primaryItemData: PrimaryItemData)
        fun loadSecondaryItems(secondaryItemData: SecondaryItemData)
        fun showCopied()
        fun loadSendButton()
        fun setSendButtonState(state: SendButtonState)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onReceiverClick()
        fun onSendError()
    }

    interface IInteractor {
        fun copyToClipboard(coinAddress: String)
    }

    interface IInteractorDelegate {
        fun didCopyToClipboard()
    }

    fun init(view: SendConfirmationViewModel, confirmationViewItems: List<SendModule.SendConfirmationViewItem>) {
        val interactor = SendConfirmationInteractor(TextHelper)
        val presenter = SendConfirmationPresenter(interactor, confirmationViewItems)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    enum class SendButtonState {
        ACTIVE, SENDING
    }

    data class PrimaryItemData(
            val primaryAmount: String,
            val secondaryAmount: String?,
            val receiver: String)

    data class SecondaryItemData(
            val memo: String?,
            val feeAmount: String?,
            val totalAmount: String?,
            val estimatedTime: String?)

}
