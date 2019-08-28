package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import io.horizontalsystems.bankwallet.modules.send.SendModule

class SendConfirmationPresenter(
        private val interactor: SendConfirmationModule.IInteractor,
        private val confirmationViewItems: List<SendModule.SendConfirmationViewItem>)
    : SendConfirmationModule.IViewDelegate, SendConfirmationModule.IInteractorDelegate {

    var view: SendConfirmationViewModel? = null

    override fun onViewDidLoad() {

//        val primaryViewItem = SendConfirmationModule.PrimaryItemData(
//                primaryAmount = confirmationInfo.primaryAmount,
//                secondaryAmount = confirmationInfo.secondaryAmount,
//                receiver = confirmationInfo.receiver
//        )
//
//        view?.loadPrimaryItem(primaryViewItem)
//
//        if (confirmationInfo.showMemo) {
//            view?.loadMemoItem()
//        }
//
//        if (confirmationInfo.fee != null || confirmationInfo.total != null || confirmationInfo.duration != null) {
//            val secondaryItemData = SendConfirmationModule.SecondaryItemData(
//                    feeAmount = confirmationInfo.fee,
//                    totalAmount = confirmationInfo.total,
//                    estimatedTime = confirmationInfo.duration)
//
//            view?.loadFeeFieldsItem(secondaryItemData)
//        }
//
//        view?.loadSendButton()
//        view?.setSendButtonState(SendConfirmationModule.SendButtonState.ACTIVE)
    }

    override fun onReceiverClick() {
//        interactor.copyToClipboard(confirmationInfo.receiver)
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

    override fun onSendClick(memo: String?) {
        view?.setSendButtonState(SendConfirmationModule.SendButtonState.SENDING)
        view?.send(memo)
    }

    override fun onSendError() {
        view?.setSendButtonState(SendConfirmationModule.SendButtonState.ACTIVE)
    }
}
