package io.horizontalsystems.bankwallet.modules.balance

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class BalanceViewModel : ViewModel(), BalanceModule.IView, BalanceModule.IRouter {

    lateinit var delegate: BalanceModule.IViewDelegate

    val openSendDialog = SingleLiveEvent<String>()
    val openReceiveDialog = SingleLiveEvent<String>()
    val balanceColorLiveDate = MutableLiveData<Int>()
    val didRefreshLiveEvent = SingleLiveEvent<Void>()
    val openManageCoinsLiveEvent = SingleLiveEvent<Void>()

    val reloadLiveEvent = SingleLiveEvent<Void>()
    val reloadHeaderLiveEvent = SingleLiveEvent<Void>()
    val reloadItemLiveEvent = SingleLiveEvent<Int>()

    fun init() {
        BalanceModule.init(this, this)

        delegate.viewDidLoad()
    }

    override fun reload() {
        reloadLiveEvent.postValue(null)
    }

    override fun updateItem(position: Int) {
        reloadItemLiveEvent.postValue(position)
    }

    override fun updateHeader() {
        reloadHeaderLiveEvent.postValue(null)
    }

    override fun openReceiveDialog(coin: String) {
        openReceiveDialog.value = coin
    }

    override fun openSendDialog(coin: String) {
        openSendDialog.value = coin
    }

    fun onReceiveClicked(position: Int) {
        delegate.onReceive(position)
    }

    fun onSendClicked(position: Int) {
        delegate.onPay(position)
    }

    override fun didRefresh() {
        didRefreshLiveEvent.call()
    }

    override fun openManageCoins() {
        openManageCoinsLiveEvent.call()
    }

    override fun onCleared() {
        delegate.onClear()
    }

}
