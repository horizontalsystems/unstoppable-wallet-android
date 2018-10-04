package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.core.managers.PreferencesManager
import bitcoin.wallet.entities.Currency
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class CurrencySwitcherInteractor(private val networkManager: NetworkManager, private val preferencesManager: PreferencesManager) : CurrencySwitcherModule.IInteractor{

    private var disposable: Disposable? = null
    private var currencyList: MutableList<CurrencyViewItem> = mutableListOf()

    var delegate: CurrencySwitcherModule.IInteractorDelegate? = null

    override fun getAvailableCurrencies() {
        disposable = networkManager.getCurrencies()
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe {
                    val baseCurrency = preferencesManager.getBaseCurrency()
                    it.forEach { item ->
                        currencyList.add(CurrencyViewItem(item, baseCurrency == item))
                    }
                    delegate?.currencyListUpdated(currencyList)
                }
    }

    override fun setBaseCurrency(currency: Currency) {
        currencyList.forEach {item ->
            item.selected = item.currency.code == currency.code
        }
        preferencesManager.setBaseCurrency(currency)
        delegate?.currencyListUpdated(currencyList)
    }
}
