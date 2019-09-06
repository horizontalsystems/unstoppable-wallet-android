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
            val primaryName: String,
            val primaryAmount: String,
            val secondaryName: String?,
            val secondaryAmount: String?,
            val receiver: String,
            val memo: String?)

    data class SecondaryItemData(
            val feeAmount: String?,
            val totalAmount: String?,
            val estimatedTime: String?)

}
