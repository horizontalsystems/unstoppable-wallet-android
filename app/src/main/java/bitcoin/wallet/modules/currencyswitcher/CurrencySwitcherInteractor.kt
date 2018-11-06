package bitcoin.wallet.modules.currencyswitcher

import bitcoin.wallet.core.ILocalStorage
import bitcoin.wallet.core.managers.NetworkManager
import bitcoin.wallet.entities.Currency
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class CurrencySwitcherInteractor(private val networkManager: NetworkManager, private val localStorage: ILocalStorage) : CurrencySwitcherModule.IInteractor{

    private var disposable: Disposable? = null
    private var currencyList: MutableList<CurrencyViewItem> = mutableListOf()

    var delegate: CurrencySwitcherModule.IInteractorDelegate? = null

    override fun getAvailableCurrencies() {
        disposable = networkManager.getCurrencies()
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe { allCurrencies ->
                    val fiatCurrencies = allCurrencies.filter { it.type.isFiat }
                    val baseCurrency = localStorage.baseCurrency
                    fiatCurrencies.forEach { item ->
                        currencyList.add(CurrencyViewItem(item, baseCurrency == item))
                    }
                    delegate?.currencyListUpdated(currencyList)
                }
    }

    override fun setBaseCurrency(currency: Currency) {
        currencyList.forEach {item ->
            item.selected = item.currency.code == currency.code
        }
        localStorage.baseCurrency = currency
        delegate?.currencyListUpdated(currencyList)
    }
}
