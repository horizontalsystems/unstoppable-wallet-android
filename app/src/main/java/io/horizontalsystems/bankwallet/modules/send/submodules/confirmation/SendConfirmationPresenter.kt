package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.SendConfirmationAmountViewItem
import io.horizontalsystems.bankwallet.modules.send.SendModule.SendConfirmationFeeViewItem
import io.horizontalsystems.bankwallet.modules.send.SendModule.SendConfirmationMemoViewItem
import io.horizontalsystems.hodler.LockTimeInterval

class SendConfirmationPresenter(
        val view: SendConfirmationModule.IView,
        private val interactor: SendConfirmationModule.IInteractor)
    : ViewModel(), SendConfirmationModule.IViewDelegate, SendConfirmationModule.IInteractorDelegate {

    private var receiver = ""
    var confirmationViewItems: List<SendModule.SendConfirmationViewItem>? = null

    override fun onViewDidLoad() {
        var primaryName = ""
        var primaryAmount = ""
        var secondaryName: String? = null
        var secondaryAmount: String? = null
        var primaryFeeAmount: String? = null
        var secondaryFeeAmount: String? = null
        var domain: String? = null
        var memo: String? = null
        var duration: Long? = null
        var lockTimeInterval: LockTimeInterval? = null

        confirmationViewItems?.forEach { item ->
            when (item) {
                is SendConfirmationAmountViewItem -> {
                    primaryName = item.primaryInfo.getAmountName()
                    primaryAmount = item.primaryInfo.getFormatted()
                    secondaryName = item.secondaryInfo?.getAmountName()
                    secondaryAmount = item.secondaryInfo?.getFormatted()
                    domain = item.receiver.domain
                    receiver = item.receiver.hex
                }
                is SendConfirmationFeeViewItem -> {
                    primaryFeeAmount = item.primaryInfo.getFormatted()
                    secondaryFeeAmount = item.secondaryInfo?.getFormatted()
                }
                is SendConfirmationMemoViewItem -> {
                    memo = item.memo
                }
                is SendModule.SendConfirmationLockTimeViewItem -> {
                    lockTimeInterval = item.lockTimeInterval
                }
            }
        }

        val primaryViewItem = SendConfirmationModule.PrimaryItemData(
                primaryName = primaryName,
                primaryAmount = primaryAmount,
                secondaryName = secondaryName,
                secondaryAmount = secondaryAmount,
                domain = domain,
                receiver = receiver,
                memo = memo,
                locked = lockTimeInterval != null
        )

        view.loadPrimaryItems(primaryViewItem)

        val secondaryViewItem = SendConfirmationModule.SecondaryItemData(
                feeAmount = primaryFeeAmount?.let { primFeeAmount ->
                    "$primFeeAmount${secondaryFeeAmount?.let { secondaryFeeAmount -> " | $secondaryFeeAmount" }
                            ?: ""}"
                },
                estimatedTime = duration,
                lockTimeInterval = lockTimeInterval
        )

        view.loadSecondaryItems(secondaryViewItem)

        view.loadSendButton()
        view.setSendButtonState(SendConfirmationModule.SendButtonState.ACTIVE)
    }

    override fun onReceiverClick() {
        interactor.copyToClipboard(receiver)
    }

    override fun didCopyToClipboard() {
        view.showCopied()
    }

    override fun onSendError() {
        view.setSendButtonState(SendConfirmationModule.SendButtonState.ACTIVE)
    }

}
