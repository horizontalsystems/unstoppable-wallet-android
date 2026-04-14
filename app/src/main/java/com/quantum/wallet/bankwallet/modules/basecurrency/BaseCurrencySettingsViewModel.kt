package com.quantum.wallet.bankwallet.modules.basecurrency

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.quantum.wallet.bankwallet.core.managers.CurrencyManager
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.Currency

class BaseCurrencySettingsViewModel(private val currencyManager: CurrencyManager) : ViewModel() {

    private val popularCurrencyCodes = listOf("USD", "EUR", "GBP", "JPY")
    private val popularCurrencies: List<Currency>
    private val otherCurrencies: List<Currency>
    private var tmpBaseCurrency: Currency? = null

    init {
        val currencies = currencyManager.currencies.toMutableList()
        val populars = mutableListOf<Currency>()

        popularCurrencyCodes.forEach { code ->
            populars.add(currencies.removeAt(currencies.indexOfFirst { it.code == code }))
        }

        popularCurrencies = populars
        otherCurrencies = currencies
    }

    private var baseCurrency: Currency
        get() = currencyManager.baseCurrency
        set(value) {
            currencyManager.baseCurrency = value
        }

    val disclaimerCurrencies = popularCurrencies.joinToString { it.code }

    var showDisclaimer by mutableStateOf(false)
        private set

    var closeScreen by mutableStateOf(false)
        private set

    val popularItems = popularCurrencies.map { CurrencyViewItem(it, it == baseCurrency) }
    val otherItems = otherCurrencies.map { CurrencyViewItem(it, it == baseCurrency) }

    fun onSelectBaseCurrency(v: Currency) {
        if (popularCurrencies.contains(v)) {
            doSetBaseCurrency(v)
        } else {
            tmpBaseCurrency = v
            showDisclaimer = true
        }
    }

    fun onAcceptDisclaimer() {
        showDisclaimer = false
        tmpBaseCurrency?.let {
            doSetBaseCurrency(it)
        }
    }

    private fun doSetBaseCurrency(v: Currency) {
        baseCurrency = v
        closeScreen = true

        stat(page = StatPage.BaseCurrency, event = StatEvent.SwitchBaseCurrency(v.code))
    }

    fun closeDisclaimer() {
        showDisclaimer = false
    }

}