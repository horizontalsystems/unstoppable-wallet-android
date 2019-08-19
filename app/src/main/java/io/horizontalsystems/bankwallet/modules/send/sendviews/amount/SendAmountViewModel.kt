package io.horizontalsystems.bankwallet.modules.send.sendviews.amount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Wallet

class SendAmountViewModel : ViewModel(), SendAmountModule.IView {

    lateinit var delegate: SendAmountModule.IViewDelegate

    val amountInputPrefixLiveData = MutableLiveData<String?>()
    val amountLiveData = MutableLiveData<String>()
    val hintLiveData = MutableLiveData<String?>()
    val maxButtonVisibleValueLiveData = MutableLiveData<Boolean>()
    val addTextChangeListenerLiveEvent = SingleLiveEvent<Unit>()
    val removeTextChangeListenerLiveEvent = SingleLiveEvent<Unit>()
    val revertAmountLiveEvent = SingleLiveEvent<String>()
    val hintErrorBalanceLiveData = MutableLiveData<String?>()
    val switchButtonEnabledLiveData = MutableLiveData<Boolean>()

    fun init(wallet: Wallet, moduleDelegate: SendAmountModule.IAmountModuleDelegate?): SendAmountPresenter {
        return SendAmountModule.init(this, wallet, moduleDelegate)
    }

    override fun setAmountType(prefix: String?) {
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

    override fun revertAmount(amount: String) {
        revertAmountLiveEvent.value = amount
    }

    override fun setHintErrorBalance(hintErrorBalance: String?) {
        hintErrorBalanceLiveData.value = hintErrorBalance
    }

    override fun setSwitchButtonEnabled(enabled: Boolean) {
        switchButtonEnabledLiveData.value = enabled
    }

}
