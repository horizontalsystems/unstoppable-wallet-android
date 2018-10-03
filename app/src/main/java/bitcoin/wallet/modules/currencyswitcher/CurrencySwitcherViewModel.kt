package bitcoin.wallet.modules.currencyswitcher

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class CurrencySwitcherViewModel: ViewModel(), CurrencySwitcherModule.IView {

    lateinit var delegate: CurrencySwitcherModule.IViewDelegate
    val currencyItems = MutableLiveData<List<CurrencyViewItem>>()


    fun init() {
        CurrencySwitcherModule.init(this)
        delegate.viewDidLoad()
    }

    override fun updateCurrencyList(currencyList: List<CurrencyViewItem>) {
        currencyItems.value = currencyList
    }
}
