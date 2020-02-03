package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import io.horizontalsystems.core.SingleLiveEvent

class SendConfirmationView: SendConfirmationModule.IView {

    val addPrimaryDataViewItem = SingleLiveEvent<SendConfirmationModule.PrimaryItemData>()
    val addSecondaryDataViewItem = SingleLiveEvent<SendConfirmationModule.SecondaryItemData>()
    val showCopied = SingleLiveEvent<Unit>()
    val addSendButton = SingleLiveEvent<Unit>()
    val sendButtonState= SingleLiveEvent<SendConfirmationModule.SendButtonState>()


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
