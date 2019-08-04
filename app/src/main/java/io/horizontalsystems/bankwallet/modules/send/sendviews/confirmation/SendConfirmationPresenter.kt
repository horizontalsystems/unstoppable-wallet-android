package io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation

class SendConfirmationPresenter(
        private val interactor: SendConfirmationModule.IInteractor,
        private val confirmationInfo: SendConfirmationInfo)
    : SendConfirmationModule.IViewDelegate, SendConfirmationModule.IInteractorDelegate {

    var view: SendConfirmationViewModel? = null

    override fun onViewDidLoad() {

        val primaryViewItem = SendConfirmationModule.PrimaryItemData(
                primaryAmount = confirmationInfo.primaryAmount,
                secondaryAmount = confirmationInfo.secondaryAmount,
                receiver = confirmationInfo.receiver
        )

        view?.loadPrimaryItem(primaryViewItem)

        if (confirmationInfo.showMemo) {
            view?.loadMemoItem()
        }

        val secondaryItemData = SendConfirmationModule.SecondaryItemData(
                feeAmount = confirmationInfo.fee,
                totalAmount = confirmationInfo.total,
                estimatedTime = confirmationInfo.time?.toString())

        view?.loadFeeFieldsItem(secondaryItemData)

        view?.loadSendButton()
        view?.setSendButtonState(SendConfirmationModule.SendButtonState.ACTIVE)
    }

    override fun onReceiverClick() {
        interactor.copyToClipboard(confirmationInfo.receiver)
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

    override fun onSendClick() {
        view?.setSendButtonState(SendConfirmationModule.SendButtonState.SENDING)
        if (confirmationInfo.showMemo) {
            view?.getMemo()
        } else {
            view?.send()
        }
    }

    override fun send(memo: String?) {
        view?.send(memo)
    }

    override fun onSendError() {
        view?.setSendButtonState(SendConfirmationModule.SendButtonState.ACTIVE)
    }
}
