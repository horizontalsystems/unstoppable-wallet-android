package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class BaseCurrencySettingsViewModel: ViewModel(), BaseCurrencySettingsModule.IBaseCurrencySettingsView {

    lateinit var delegate: BaseCurrencySettingsModule.IBaseCurrencySettingsViewDelegate
    val currencyItems = MutableLiveData<List<CurrencyItem>>()
    val closeLiveEvent = SingleLiveEvent<Unit>()


    fun init() {
        BaseCurrencySettingsModule.init(this)
        delegate.viewDidLoad()
    }

    override fun show(items: List<CurrencyItem>) {
        currencyItems.value = items
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
