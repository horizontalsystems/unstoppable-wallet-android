package io.horizontalsystems.bankwallet.modules.wallet

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.CurrencyValue

class WalletViewModel : ViewModel(), WalletModule.IView, WalletModule.IRouter {

    lateinit var delegate: WalletModule.IViewDelegate

    val titleLiveDate = MutableLiveData<Int>()
    val walletsLiveData = MutableLiveData<List<WalletViewItem>>()
    val totalBalanceLiveData = MutableLiveData<CurrencyValue>()
    val openSendDialog = SingleLiveEvent<String>()
    val openReceiveDialog = SingleLiveEvent<String>()
    val balanceColorLiveDate = MutableLiveData<Int>()

    fun init() {
        WalletModule.init(this, this)

        delegate.viewDidLoad()
    }

    override fun openReceiveDialog(coin: String) {
        openReceiveDialog.value = coin
    }

    override fun openSendDialog(coin: String) {
        openSendDialog.value = coin
    }

    fun onReceiveClicked(adapterId: String) {
        delegate.onReceive(adapterId)
    }

    fun onSendClicked(adapterId: String) {
        delegate.onPay(adapterId)
    }

    override fun setTitle(title: Int) {
        titleLiveDate.value = title
    }

    override fun didRefresh() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun show(totalBalance: CurrencyValue?) {
        totalBalanceLiveData.value = totalBalance
    }

    override fun show(wallets: List<WalletViewItem>) {
        walletsLiveData.value = wallets
    }

    override fun show(syncStatus: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateBalanceColor(color: Int) {
        balanceColorLiveDate.value = color
    }
}
