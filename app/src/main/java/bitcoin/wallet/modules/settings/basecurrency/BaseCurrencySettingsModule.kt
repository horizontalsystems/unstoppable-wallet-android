package bitcoin.wallet.modules.settings.basecurrency

import android.content.Context
import android.content.Intent
import bitcoin.wallet.core.App
import bitcoin.wallet.entities.Currency

object BaseCurrencySettingsModule {

    interface IBaseCurrencySettingsView{
        fun show(items: List<CurrencyItem>)
    }

    interface IBaseCurrencySettingsViewDelegate {
        fun viewDidLoad()
        fun didSelect(item: CurrencyItem)
    }

    interface IBaseCurrencySettingsInteractor {
        val currencies: List<Currency>
        val baseCurrency: Currency
        fun setBaseCurrency(code: String)
    }

    interface IBaseCurrencySettingsInteractorDelegate {
        fun didSetBaseCurrency()
    }

    fun init(view: BaseCurrencySettingsViewModel) {
        val interactor = BaseCurrencySettingsInteractor(App.currencyManager)
        val presenter = BaseCurrencySettingsPresenter(interactor)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        val intent = Intent(context, BaseCurrencySettingsActivity::class.java)
        context.startActivity(intent)
    }

}

data class CurrencyItem (val code: String, val symbol: String, val selected: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (other is CurrencyItem) {
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
