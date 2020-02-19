package io.horizontalsystems.currencyswitcher

import androidx.lifecycle.MutableLiveData

class BaseCurrencySettingsView : BaseCurrencySettingsModule.IView {
    val currencyItems = MutableLiveData<List<CurrencyViewItem>>()

    override fun show(items: List<CurrencyViewItem>) {
        currencyItems.value = items
    }
}
