package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class BalanceXRateRepository(
    private val tag: String,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper
) {
    val baseCurrency by currencyManager::baseCurrency
    private var coinUids = listOf<String>()

    private var latestRateDisposable: Disposable? = null
    private var baseCurrencyDisposable: Disposable? = null

    private val itemSubject = PublishSubject.create<Map<String, CoinPrice?>>()
    val itemObservable: Observable<Map<String, CoinPrice?>> get() = itemSubject
        .doOnSubscribe {
            subscribeForBaseCurrencyUpdate()
            subscribeForLatestRateUpdates()
        }
        .doFinally {
            unsubscribeFromBaseCurrencyUpdate()
            unsubscribeFromLatestRateUpdates()
        }

    private fun subscribeForBaseCurrencyUpdate() {
        baseCurrencyDisposable = currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                unsubscribeFromLatestRateUpdates()
                itemSubject.onNext(getLatestRates())
                subscribeForLatestRateUpdates()
            }
    }

    private fun unsubscribeFromBaseCurrencyUpdate() {
        baseCurrencyDisposable?.dispose()
    }

    fun setCoinUids(coinUids: List<String>) {
        unsubscribeFromLatestRateUpdates()
        this.coinUids = coinUids
        subscribeForLatestRateUpdates()
    }

    fun getLatestRates(): Map<String, CoinPrice?> {
        return coinUids.associateWith { null } + marketKit.coinPriceMap(coinUids, baseCurrency.code)
    }

    fun refresh() {
        marketKit.refreshCoinPrices(baseCurrency.code)
    }

    private fun subscribeForLatestRateUpdates() {
        latestRateDisposable = marketKit.coinPriceMapObservable(tag, coinUids, baseCurrency.code)
            .subscribeIO {
                itemSubject.onNext(it)
            }
    }

    private fun unsubscribeFromLatestRateUpdates() {
        latestRateDisposable?.dispose()
    }
}