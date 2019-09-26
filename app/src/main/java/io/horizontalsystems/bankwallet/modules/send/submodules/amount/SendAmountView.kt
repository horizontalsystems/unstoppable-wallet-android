package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.SingleLiveEvent

class SendAmountView : SendAmountModule.View {

    val amount = MutableLiveData<String>()
    val hint = MutableLiveData<String?>()
    val amountInputPrefix = MutableLiveData<String?>()
    val maxButtonVisibleValue = MutableLiveData<Boolean>()
    val addTextChangeListener = SingleLiveEvent<Unit>()
    val removeTextChangeListener = SingleLiveEvent<Unit>()
    val revertAmount = SingleLiveEvent<String>()
    val hintErrorBalance = MutableLiveData<String?>()
    val switchButtonEnabled = MutableLiveData<Boolean>()

    override fun setAmountType(prefix: String?) {
        amountInputPrefix.value = prefix
    }

    override fun setAmount(amount: String) {
        this.amount.value = amount
    }

    override fun setHint(hint: String?) {
        this.hint.value = hint
    }

    override fun setMaxButtonVisible(visible: Boolean) {
        maxButtonVisibleValue.value = visible
    }

    override fun addTextChangeListener() {
        addTextChangeListener.call()
    }

    override fun removeTextChangeListener() {
        removeTextChangeListener.call()
    }

    override fun revertAmount(amount: String) {
        revertAmount.value = amount
    }

    override fun setHintErrorBalance(hintErrorBalance: String?) {
        this.hintErrorBalance.value = hintErrorBalance
    }

    override fun setSwitchButtonEnabled(enabled: Boolean) {
        switchButtonEnabled.value = enabled
    }
}
