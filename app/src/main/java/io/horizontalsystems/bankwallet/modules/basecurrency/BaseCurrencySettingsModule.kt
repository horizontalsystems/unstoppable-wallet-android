package io.horizontalsystems.bankwallet.modules.basecurrency

import io.horizontalsystems.bankwallet.entities.Currency

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
