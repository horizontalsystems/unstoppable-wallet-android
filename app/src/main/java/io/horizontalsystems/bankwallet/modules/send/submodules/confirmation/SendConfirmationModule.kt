package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

object SendConfirmationModule {

    const val ConfirmationInfoKey = "confirmation_info_key"

    interface IView{
        fun loadPrimaryItem(primaryItemData: PrimaryItemData)
        fun loadMemoItem()
        fun showCopied()
        fun loadFeeFieldsItem(secondaryItemData: SecondaryItemData)
        fun loadSendButton()
        fun getMemo()
        fun send(memo: String? = null)
        fun setSendButtonState(state: SendButtonState)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onReceiverClick()
        fun onSendClick(memo: String?)
        fun onSendError()
    }

    interface IInteractor {
        fun copyToClipboard(coinAddress: String)
    }

    interface IInteractorDelegate {
        fun didCopyToClipboard()
    }

    fun init(view: SendConfirmationViewModel, confirmationInfo: SendConfirmationInfo) {
        val interactor = SendConfirmationInteractor(TextHelper)
        val presenter = SendConfirmationPresenter(interactor, confirmationInfo)

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
            val feeAmount: String?,
            val totalAmount: String?,
            val estimatedTime: String?)

}
