package bitcoin.wallet.modules.wallet

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.WalletBalanceViewItem

class WalletViewModel : ViewModel(), WalletModule.IView, WalletModule.IRouter {

    lateinit var delegate: WalletModule.IViewDelegate

    val walletBalancesLiveData = MutableLiveData<List<WalletBalanceViewItem>>()
    val totalBalanceLiveData = MutableLiveData<CurrencyValue>()

    fun init() {
        WalletModule.init(this, this)

        delegate.viewDidLoad()
    }

    override fun onCleared() {
        super.onCleared()

        WalletModule.destroy()
    }

    override fun showTotalBalance(totalBalance: CurrencyValue) {
        totalBalanceLiveData.value = totalBalance
    }

    override fun showWalletBalances(walletBalances: List<WalletBalanceViewItem>) {
        walletBalancesLiveData.value = walletBalances
    }

}
