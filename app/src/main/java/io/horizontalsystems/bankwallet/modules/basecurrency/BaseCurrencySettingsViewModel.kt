package io.horizontalsystems.bankwallet.modules.basecurrency

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.views.ListPosition

class BaseCurrencySettingsViewModel(private val service: BaseCurrencySettingsService) : ViewModel() {

    val disclaimerLiveEvent = SingleLiveEvent<String>()
    val finishLiveEvent = SingleLiveEvent<Unit>()

    private var tmpBaseCurrency: Currency? = null

    val popularItems = service.popularCurrencies.mapIndexed { index, currency ->
        CurrencyViewItemWrapper(
                currency,
                currency == service.baseCurrency,
                ListPosition.getListPosition(service.popularCurrencies.size, index)
        )
    }

    val otherItems = service.otherCurrencies.mapIndexed { index, currency ->
        CurrencyViewItemWrapper(
                currency,
                currency == service.baseCurrency,
                ListPosition.getListPosition(service.otherCurrencies.size, index)
        )
    }

    fun setBaseCurrency(v: Currency) {
        if (service.popularCurrencies.contains(v)) {
            doSetBaseCurrency(v)
        } else {
            tmpBaseCurrency = v
            disclaimerLiveEvent.postValue(service.popularCurrencies.map { it.code }.joinToString())
        }
    }

    fun onAcceptDisclaimer() {
        tmpBaseCurrency?.let {
            doSetBaseCurrency(it)
        }
    }

    private fun doSetBaseCurrency(v: Currency) {
        service.baseCurrency = v
        finishLiveEvent.call()
    }

}