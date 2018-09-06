package bitcoin.wallet.modules.wallet

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.SingleLiveEvent
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.coins.Coin

class WalletViewModel : ViewModel(), WalletModule.IView, WalletModule.IRouter {

    lateinit var delegate: WalletModule.IViewDelegate

    val walletBalancesLiveData = MutableLiveData<List<WalletBalanceViewItem>>()
    val totalBalanceLiveData = MutableLiveData<CurrencyValue>()
    val openSendDialog = SingleLiveEvent<Coin>()
    val openReceiveDialog = SingleLiveEvent<String>()

    fun init() {
        WalletModule.init(this, this)

        delegate.viewDidLoad()
    }

    override fun onCleared() {
        super.onCleared()

        WalletModule.destroy()
    }

    override fun openReceiveDialog(adapterId: String) {
        openReceiveDialog.value = adapterId
    }

    override fun openSendDialog(coin: Coin) {
        openSendDialog.value = coin
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

    fun onSendClicked(coin: Coin) {
        delegate.onSendClicked(coin)
    }

}
