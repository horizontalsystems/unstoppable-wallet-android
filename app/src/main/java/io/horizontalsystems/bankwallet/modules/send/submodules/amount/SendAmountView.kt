package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.core.SingleLiveEvent

class SendAmountView : SendAmountModule.IView {

    val amount = MutableLiveData<String>()
    val availableBalance = MutableLiveData<String>()
    val hint = MutableLiveData<String>()
    val hintStateEnabled = MutableLiveData<Boolean>()
    val amountInputPrefix = MutableLiveData<String>()
    val maxButtonVisibleValue = MutableLiveData<Boolean>()
    val addTextChangeListener = SingleLiveEvent<Unit>()
    val removeTextChangeListener = SingleLiveEvent<Unit>()
    val revertAmount = SingleLiveEvent<String>()
    val validationError = MutableLiveData<SendAmountModule.ValidationError?>()
    val switchButtonEnabled = MutableLiveData<Boolean>()
    val setLoading = MutableLiveData<Boolean>()

    override fun setAmountType(prefix: String) {
        amountInputPrefix.value = prefix
    }

    override fun setAmount(amount: String) {
        this.amount.postValue(amount)
    }

    override fun setAvailableBalance(availableBalance: String) {
        this.availableBalance.postValue(availableBalance)
    }

    override fun setHint(hint: String) {
        this.hint.postValue(hint)
    }

    override fun setHintStateEnabled(enabled: Boolean) {
        hintStateEnabled.postValue(enabled)
    }

    override fun setMaxButtonVisible(visible: Boolean) {
        maxButtonVisibleValue.postValue(visible)
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

    override fun setValidationError(error: SendAmountModule.ValidationError?) {
        this.validationError.postValue(error)
    }

    override fun setSwitchButtonEnabled(enabled: Boolean) {
        switchButtonEnabled.postValue(enabled)
    }

    override fun setLoading(loading: Boolean) {
        setLoading.postValue(loading)
    }
}
