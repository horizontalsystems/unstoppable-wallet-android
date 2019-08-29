package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.SendConfirmationAmountViewItem
import io.horizontalsystems.bankwallet.modules.send.SendModule.SendConfirmationDurationViewItem
import io.horizontalsystems.bankwallet.modules.send.SendModule.SendConfirmationFeeViewItem
import io.horizontalsystems.bankwallet.modules.send.SendModule.SendConfirmationMemoViewItem

class SendConfirmationPresenter(
        private val interactor: SendConfirmationModule.IInteractor,
        private val confirmationViewItems: List<SendModule.SendConfirmationViewItem>)
    : SendConfirmationModule.IViewDelegate, SendConfirmationModule.IInteractorDelegate {

    var view: SendConfirmationViewModel? = null
    private var receiver = ""

    override fun onViewDidLoad() {
        var primaryAmount = ""
        var secondaryAmount = ""
        var primaryFeeAmount: String? = null
        var secondaryFeeAmount: String? = null
        var memo: String? = null
        var duration: String? = null

        confirmationViewItems.forEach { item ->
            when (item) {
                is SendConfirmationAmountViewItem -> {
                    primaryAmount = item.primaryInfo.getFormatted() ?: ""
                    secondaryAmount = item.secondaryInfo?.getFormatted() ?: ""
                    receiver = item.receiver
                }
                is SendConfirmationFeeViewItem -> {
                    primaryFeeAmount = item.primaryInfo.getFormatted()
                    secondaryFeeAmount = item.secondaryInfo?.getFormatted()
                }
                is SendConfirmationMemoViewItem -> {
                    memo = item.memo
                }
                is SendConfirmationDurationViewItem -> {
                    duration = item.duration
                }
            }
        }

        val primaryViewItem = SendConfirmationModule.PrimaryItemData(
                primaryAmount = primaryAmount,
                secondaryAmount = secondaryAmount,
                receiver = receiver
        )

        view?.loadPrimaryItems(primaryViewItem)

        val secondaryViewItem = SendConfirmationModule.SecondaryItemData(
                memo = memo,
                feeAmount = primaryFeeAmount?.let { primaryFeeAmount ->
                    "$primaryFeeAmount${secondaryFeeAmount?.let { secondaryFeeAmount -> " | $secondaryFeeAmount" }
                            ?: ""}"
                },
                totalAmount = null,
                estimatedTime = duration
        )

        view?.loadSecondaryItems(secondaryViewItem)

        view?.loadSendButton()
        view?.setSendButtonState(SendConfirmationModule.SendButtonState.ACTIVE)
    }

    override fun onReceiverClick() {
        interactor.copyToClipboard(receiver)
    }

    override fun didCopyToClipboard() {
        view?.showCopied()
    }

    override fun onSendError() {
        view?.setSendButtonState(SendConfirmationModule.SendButtonState.ACTIVE)
    }

}
