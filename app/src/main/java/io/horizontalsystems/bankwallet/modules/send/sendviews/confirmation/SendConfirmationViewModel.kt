package io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class SendConfirmationViewModel: ViewModel(), SendConfirmationModule.IView {

    lateinit var delegate: SendConfirmationModule.IViewDelegate

    val addPrimaryDataViewItem = SingleLiveEvent<SendConfirmationModule.PrimaryItemData>()
    val addSecondaryDataViewItem = SingleLiveEvent<SendConfirmationModule.SecondaryItemData>()
    val addMemoViewItem = SingleLiveEvent<Unit>()
    val showCopied = SingleLiveEvent<Unit>()
    val addSendButton = SingleLiveEvent<Unit>()
    val getMemo = SingleLiveEvent<Unit>()
    val sendWithMemo = SingleLiveEvent<String?>()
    val sendButtonState= SingleLiveEvent<SendConfirmationModule.SendButtonState>()


    fun init(confirmationInfo: SendConfirmationInfo) {
        SendConfirmationModule.init(this, confirmationInfo)
        delegate.onViewDidLoad()
    }

    override fun loadPrimaryItem(primaryItemData: SendConfirmationModule.PrimaryItemData) {
        addPrimaryDataViewItem.value = primaryItemData
    }

    override fun loadMemoItem() {
        addMemoViewItem.call()
    }

    override fun showCopied() {
        showCopied.call()
    }

    override fun loadFeeFieldsItem(secondaryItemData: SendConfirmationModule.SecondaryItemData) {
        addSecondaryDataViewItem.value = secondaryItemData
    }

    override fun loadSendButton() {
        addSendButton.call()
    }

    override fun getMemo() {
        getMemo.call()
    }

    override fun send(memo: String?) {
        sendWithMemo.value = memo
    }

    override fun setSendButtonState(state: SendConfirmationModule.SendButtonState) {
        sendButtonState.value = state
    }
}
