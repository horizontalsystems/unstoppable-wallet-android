package io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation

class SendConfirmationPresenter(
        private val interactor: SendConfirmationModule.IInteractor,
        private val confirmationInfo: SendConfirmationInfo)
    : SendConfirmationModule.IViewDelegate, SendConfirmationModule.IInteractorDelegate {

    var view: SendConfirmationViewModel? = null

    override fun onViewDidLoad() {
        confirmationInfo.primaryAmount

        val primaryViewItem = SendConfirmationModule.PrimaryItemData(
                confirmationInfo.primaryAmount,
                confirmationInfo.secondaryAmount,
                confirmationInfo.receiver
        )

        view?.loadPrimaryItem(primaryViewItem)

        if (confirmationInfo.showMemo) {
            view?.loadMemoItem()
        }

        view?.loadFeeFieldsItem(SendConfirmationModule.SecondaryItemData(confirmationInfo.fee, confirmationInfo.total, confirmationInfo.time?.toString()))

        view?.loadSendButton()
    }

    override fun onReceiverClick() {
        interactor.copyToClipboard(confirmationInfo.receiver)
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

    override fun onSendClick() {
        view?.getMemoForSend()
    }

    override fun send(memo: String?) {
        view?.sendWithInput(memo)
    }
}
