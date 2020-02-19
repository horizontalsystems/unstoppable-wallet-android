package io.horizontalsystems.currencyswitcher

import androidx.lifecycle.MutableLiveData

class CurrencySwitcherView : CurrencySwitcherModule.IView {
    val currencyItems = MutableLiveData<List<CurrencyViewItem>>()

    override fun show(items: List<CurrencyViewItem>) {
        currencyItems.value = items
    }
}
