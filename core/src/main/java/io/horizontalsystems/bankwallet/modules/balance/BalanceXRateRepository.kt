package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class BalanceXRateRepository(
    private val tag: String,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper
) {
    val baseCurrency by currencyManager::baseCurrency
    private var coinUids = listOf<String>()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var latestRateJob: Job? = null
    private var baseCurrencyJob: Job? = null

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
        baseCurrencyJob = coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                unsubscribeFromLatestRateUpdates()
                itemSubject.onNext(getLatestRates())
                subscribeForLatestRateUpdates()
            }
        }
    }

    private fun unsubscribeFromBaseCurrencyUpdate() {
        baseCurrencyJob?.cancel()
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
        latestRateJob = coroutineScope.launch {
            marketKit.coinPriceMapObservable(tag, coinUids, baseCurrency.code).asFlow().collect {
                itemSubject.onNext(it)
            }
        }
    }

    private fun unsubscribeFromLatestRateUpdates() {
        latestRateJob?.cancel()
    }
}