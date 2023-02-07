package cash.p.terminal.modules.basecurrency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.Currency

object BaseCurrencySettingsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BaseCurrencySettingsViewModel(App.currencyManager) as T
        }
    }
}

data class CurrencyViewItem(val currency: Currency, val selected: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (other is CurrencyViewItem) {
            return currency == other.currency
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return currency.hashCode()
    }
}
