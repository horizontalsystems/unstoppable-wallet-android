package bitcoin.wallet.modules.settings.basecurrency

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class BaseCurrencySettingsViewModel: ViewModel(), BaseCurrencySettingsModule.IBaseCurrencySettingsView {

    lateinit var delegate: BaseCurrencySettingsModule.IBaseCurrencySettingsViewDelegate
    val currencyItems = MutableLiveData<List<CurrencyItem>>()


    fun init() {
        BaseCurrencySettingsModule.init(this)
        delegate.viewDidLoad()
    }

    override fun show(items: List<CurrencyItem>) {
        currencyItems.value = items
    }

}
