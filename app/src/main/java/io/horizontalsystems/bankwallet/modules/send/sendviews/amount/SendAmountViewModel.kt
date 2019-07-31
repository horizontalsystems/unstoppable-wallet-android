package io.horizontalsystems.bankwallet.modules.send.sendviews.amount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal

class SendAmountViewModel: ViewModel(), SendAmountModule.IView {

    lateinit var delegate: SendAmountModule.IViewDelegate

    val amountInputPrefixLiveData = MutableLiveData<String>()
    val amountLiveData = MutableLiveData<String>()
    val hintLiveData = MutableLiveData<String?>()
    val maxButtonVisibleValueLiveData = MutableLiveData<Boolean>()
    val addTextChangeListenerLiveEvent = SingleLiveEvent<Unit>()
    val removeTextChangeListenerLiveEvent = SingleLiveEvent<Unit>()
    val revertInputLiveEvent = SingleLiveEvent<String>()
    val getAvailableBalanceLiveEvent = SingleLiveEvent<Unit>()
    val notifyMainViewModelOnAmountChangeLiveData = MutableLiveData<BigDecimal>()
    val hintErrorBalanceLiveData = MutableLiveData<String?>()
    val switchButtonEnabledLiveData = MutableLiveData<Boolean>()

    fun init(coinCode: String) {
        SendAmountModule.init(this, coinCode)
    }

    override fun setAmountPrefix(prefix: String?) {
        amountInputPrefixLiveData.value = prefix
    }

    override fun setAmount(amount: String) {
        amountLiveData.value = amount
    }

    override fun setHint(hint: String?) {
        hintLiveData.value = hint
    }

    override fun setMaxButtonVisible(visible: Boolean) {
        maxButtonVisibleValueLiveData.value = visible
    }

    override fun addTextChangeListener() {
        addTextChangeListenerLiveEvent.call()
    }

    override fun removeTextChangeListener() {
        removeTextChangeListenerLiveEvent.call()
    }

    override fun revertInput(revertedInput: String) {
        revertInputLiveEvent.value = revertedInput
    }

    override fun getAvailableBalance() {
        getAvailableBalanceLiveEvent.call()
    }

    override fun notifyMainViewModelOnAmountChange(coinAmount: BigDecimal?) {
        notifyMainViewModelOnAmountChangeLiveData.value = coinAmount
    }

    override fun setHintErrorBalance(hintErrorBalance: String?) {
        hintErrorBalanceLiveData.value = hintErrorBalance
    }

    override fun setSwitchButtonEnabled(enabled: Boolean) {
        switchButtonEnabledLiveData.value = enabled
    }
}
