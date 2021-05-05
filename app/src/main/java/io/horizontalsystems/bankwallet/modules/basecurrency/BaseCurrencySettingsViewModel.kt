package io.horizontalsystems.bankwallet.modules.basecurrency

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.views.ListPosition

class BaseCurrencySettingsViewModel(private val service: BaseCurrencySettingsService) : ViewModel() {

    val finishLiveEvent = SingleLiveEvent<Unit>()

    var baseCurrency: Currency
        get() = service.baseCurrency
        set(value) {
            service.baseCurrency = value

            finishLiveEvent.call()
        }

    val popularItems = service.popularCurrencies.mapIndexed { index, currency ->
        CurrencyViewItemWrapper(
                currency,
                currency == baseCurrency,
                ListPosition.getListPosition(service.popularCurrencies.size, index)
        )
    }

    val otherItems = service.otherCurrencies.mapIndexed { index, currency ->
        CurrencyViewItemWrapper(
                currency,
                currency == baseCurrency,
                ListPosition.getListPosition(service.otherCurrencies.size, index)
        )
    }
}