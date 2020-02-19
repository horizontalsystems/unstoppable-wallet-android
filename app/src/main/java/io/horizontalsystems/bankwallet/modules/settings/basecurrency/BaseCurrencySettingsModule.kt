package io.horizontalsystems.bankwallet.modules.settings.basecurrency

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.entities.Currency

object BaseCurrencySettingsModule {

    interface IView {
        fun show(items: List<CurrencyViewItem>)
    }

    interface IRouter {
        fun close()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didSelect(position: Int)
    }

    interface IInteractor {
        val currencies: List<Currency>
        var baseCurrency: Currency
    }

    fun start(context: Context) {
        val intent = Intent(context, BaseCurrencySettingsActivity::class.java)
        context.startActivity(intent)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = BaseCurrencySettingsView()
            val router = BaseCurrencySettingsRouter()
            val interactor = BaseCurrencySettingsInteractor(App.currencyManager)
            val presenter = BaseCurrencySettingsPresenter(view, router, interactor)

            return presenter as T
        }
    }
}

data class CurrencyViewItem(val code: String, val symbol: String, val selected: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (other is CurrencyViewItem) {
            return code == other.code
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + symbol.hashCode()
        result = 31 * result + selected.hashCode()
        return result
    }
}
