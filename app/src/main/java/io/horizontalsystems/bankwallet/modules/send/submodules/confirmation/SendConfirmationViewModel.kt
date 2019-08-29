package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.send.SendModule

class SendConfirmationViewModel: ViewModel(), SendConfirmationModule.IView {

    lateinit var delegate: SendConfirmationModule.IViewDelegate

    val addPrimaryDataViewItem = SingleLiveEvent<SendConfirmationModule.PrimaryItemData>()
    val addSecondaryDataViewItem = SingleLiveEvent<SendConfirmationModule.SecondaryItemData>()
    val showCopied = SingleLiveEvent<Unit>()
    val addSendButton = SingleLiveEvent<Unit>()
    val sendButtonState= SingleLiveEvent<SendConfirmationModule.SendButtonState>()


    fun init(confirmationInfo: List<SendModule.SendConfirmationViewItem>) {
        SendConfirmationModule.init(this, confirmationInfo)
        delegate.onViewDidLoad()
    }

    override fun loadPrimaryItems(primaryItemData: SendConfirmationModule.PrimaryItemData) {
        addPrimaryDataViewItem.value = primaryItemData
    }

    override fun showCopied() {
        showCopied.call()
    }

    override fun loadSecondaryItems(secondaryItemData: SendConfirmationModule.SecondaryItemData) {
        addSecondaryDataViewItem.value = secondaryItemData
    }

    override fun loadSendButton() {
        addSendButton.call()
    }

    override fun setSendButtonState(state: SendConfirmationModule.SendButtonState) {
        sendButtonState.value = state
    }
}
