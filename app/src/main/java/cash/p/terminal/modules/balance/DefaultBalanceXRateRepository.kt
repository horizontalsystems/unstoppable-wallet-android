package cash.p.terminal.modules.balance

import io.horizontalsystems.core.CurrencyManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.balance.BalanceXRateRepository
import cash.p.terminal.wallet.models.CoinPrice
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class DefaultBalanceXRateRepository(
    private val tag: String,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper
) : BalanceXRateRepository {
    override val baseCurrency by currencyManager::baseCurrency
    private var coinUids = listOf<String>()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var latestRateJob: Job? = null
    private var baseCurrencyJob: Job? = null

    private val itemSubject = PublishSubject.create<Map<String, CoinPrice?>>()
    override val itemObservable: Observable<Map<String, CoinPrice?>> get() = itemSubject
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

    override fun setCoinUids(coinUids: List<String>) {
        unsubscribeFromLatestRateUpdates()
        this.coinUids = coinUids
        subscribeForLatestRateUpdates()
    }

    override fun getLatestRates(): Map<String, CoinPrice?> {
        return coinUids.associateWith { null } + marketKit.coinPriceMap(coinUids, baseCurrency.code)
    }

    override fun refresh() {
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