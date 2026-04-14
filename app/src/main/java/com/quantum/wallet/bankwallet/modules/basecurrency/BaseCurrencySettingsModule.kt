package com.quantum.wallet.bankwallet.modules.basecurrency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Currency

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
