package io.horizontalsystems.bankwallet.modules.balance

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.CurrencyValue

class BalanceViewModel : ViewModel(), BalanceModule.IView, BalanceModule.IRouter {

    lateinit var delegate: BalanceModule.IViewDelegate

    val titleLiveDate = MutableLiveData<Int>()
    val totalBalanceLiveData = MutableLiveData<CurrencyValue>()
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
        reloadLiveEvent.callX()
    }

    override fun updateItem(position: Int) {
        reloadItemLiveEvent.callX(position)
    }

    override fun updateHeader() {
        reloadHeaderLiveEvent.callX()
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

//    override fun setTitle(title: Int) {
//        titleLiveDate.value = title
//    }
//
    override fun didRefresh() {
        didRefreshLiveEvent.call()
    }
//
//    override fun show(totalBalance: CurrencyValue?) {
//        totalBalanceLiveData.value = totalBalance
//    }
//
//    override fun show(wallets: List<BalanceViewItem>) {
//        walletsLiveData.value = wallets
//    }
//
//    override fun show(syncStatus: String) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun updateBalanceColor(color: Int) {
//        balanceColorLiveDate.value = color
//    }

    override fun openManageCoins() {
        openManageCoinsLiveEvent.call()
    }
}
