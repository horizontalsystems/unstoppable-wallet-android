package bitcoin.wallet.modules.wallet

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent
import bitcoin.wallet.core.IAdapter
import bitcoin.wallet.entities.CurrencyValue

class WalletViewModel : ViewModel(), WalletModule.IView, WalletModule.IRouter {

    lateinit var delegate: WalletModule.IViewDelegate

    val walletBalancesLiveData = MutableLiveData<List<WalletBalanceViewItem>>()
    val totalBalanceLiveData = MutableLiveData<CurrencyValue>()
    val openSendDialog = SingleLiveEvent<IAdapter>()
    val openReceiveDialog = SingleLiveEvent<String>()
    val navigateToSetPin = SingleLiveEvent<Unit>()

    fun init() {
        WalletModule.init(this, this)

        delegate.viewDidLoad()
    }

    override fun openReceiveDialog(adapterId: String) {
        openReceiveDialog.value = adapterId
    }

    override fun openSendDialog(adapter: IAdapter) {
        openSendDialog.value = adapter
    }

    override fun showTotalBalance(totalBalance: CurrencyValue) {
        totalBalanceLiveData.value = totalBalance
    }

    override fun showWalletBalances(walletBalances: List<WalletBalanceViewItem>) {
        walletBalancesLiveData.value = walletBalances
    }

    fun onReceiveClicked(adapterId: String) {
        delegate.onReceiveClicked(adapterId)
    }

    fun onSendClicked(adapterId: String) {
        delegate.onSendClicked(adapterId)
    }

    override fun navigateToSetPin() {
        navigateToSetPin.call()
    }

}
