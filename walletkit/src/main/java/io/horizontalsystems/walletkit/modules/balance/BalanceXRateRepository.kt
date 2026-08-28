package io.horizontalsystems.walletkit.modules.balance

import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

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

    private val itemFlow = MutableSharedFlow<Map<String, CoinPrice?>>()
    val itemObservable: Flow<Map<String, CoinPrice?>> get() = itemFlow
        .onStart {
            subscribeForBaseCurrencyUpdate()
            subscribeForLatestRateUpdates()
        }
        .onCompletion {
            unsubscribeFromBaseCurrencyUpdate()
            unsubscribeFromLatestRateUpdates()
        }

    private fun subscribeForBaseCurrencyUpdate() {
        baseCurrencyJob?.cancel()
        baseCurrencyJob = coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                unsubscribeFromLatestRateUpdates()
                itemFlow.emit(getLatestRates())
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
        latestRateJob?.cancel()
        latestRateJob = coroutineScope.launch {
            marketKit.coinPriceMapObservable(tag, coinUids, baseCurrency.code).collect {
                itemFlow.emit(it)
            }
        }
    }

    private fun unsubscribeFromLatestRateUpdates() {
        latestRateJob?.cancel()
    }
}