package io.horizontalsystems.bankwallet.modules.ratelist

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.IWalletStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class RatesInteractor(
        private val xRateManager: IRateManager,
        private val currencyManager: ICurrencyManager,
        private val walletStorage: IWalletStorage,
        private val appConfigProvider: IAppConfigProvider,
        private val rateListSorter: RateListSorter)
    : RateListModule.IInteractor {

    var delegate: RateListModule.IInteractorDelegate? = null
    private var disposables = CompositeDisposable()

    override val currency: Currency
        get() = currencyManager.baseCurrency

    override val coins: List<Coin>
        get() = rateListSorter.smartSort(walletStorage.enabledCoins(), appConfigProvider.featuredCoins)

    override fun setupXRateManager(coinCodes: List<String>) {
        xRateManager.set(coinCodes)
    }

    override fun getMarketInfo(coinCode: String, currencyCode: String): MarketInfo? {
        return xRateManager.marketInfo(coinCode, currencyCode)
    }

    override fun subscribeToMarketInfo(currencyCode: String) {
        xRateManager.marketInfoObservable(currencyCode)
                .subscribeOn(Schedulers.io())
                .subscribe({ marketInfo ->
                    delegate?.didUpdateMarketInfo(marketInfo)
                }, { /*throwable*/ })
                .let { disposables.add(it) }
    }

    override fun getTopList() {
        xRateManager.getTopMarketList(currency.code)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    delegate?.didFetchedTopMarketList(it)
                }, {
                    delegate?.didFailToFetchTopList()
                } )
                .let { disposables.add(it) }
    }

    override fun clear() {
        disposables.clear()
    }
}
