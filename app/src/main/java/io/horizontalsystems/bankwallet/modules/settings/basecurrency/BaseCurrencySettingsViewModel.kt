package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class BaseCurrencySettingsViewModel: ViewModel(), BaseCurrencySettingsModule.IView {

    lateinit var delegate: BaseCurrencySettingsModule.IViewDelegate
    val currencyItems = MutableLiveData<List<CurrencyViewItem>>()
    val closeLiveEvent = SingleLiveEvent<Unit>()


    fun init() {
        BaseCurrencySettingsModule.init(this)
        delegate.viewDidLoad()
    }

    override fun show(items: List<CurrencyViewItem>) {
        currencyItems.value = items
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
