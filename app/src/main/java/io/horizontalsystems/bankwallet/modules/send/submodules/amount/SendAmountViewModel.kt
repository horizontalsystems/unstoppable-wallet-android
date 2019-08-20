package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Wallet

class SendAmountViewModel : ViewModel(), SendAmountModule.IView {

    lateinit var delegate: SendAmountModule.IViewDelegate

    val amountInputPrefix = MutableLiveData<String?>()
    val amount = MutableLiveData<String>()
    val hint = MutableLiveData<String?>()
    val maxButtonVisibleValue = MutableLiveData<Boolean>()
    val addTextChangeListener = SingleLiveEvent<Unit>()
    val removeTextChangeListener = SingleLiveEvent<Unit>()
    val revertAmount = SingleLiveEvent<String>()
    val hintErrorBalance = MutableLiveData<String?>()
    val switchButtonEnabled = MutableLiveData<Boolean>()

    fun init(wallet: Wallet, moduleDelegate: SendAmountModule.IAmountModuleDelegate?): SendAmountPresenter {
        return SendAmountModule.init(this, wallet, moduleDelegate)
    }

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
