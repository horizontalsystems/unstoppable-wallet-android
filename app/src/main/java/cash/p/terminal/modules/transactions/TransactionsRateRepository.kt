package cash.p.terminal.modules.transactions

import android.util.Log
import cash.p.terminal.core.Clearable
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.CurrencyValue
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal

class TransactionsRateRepository(
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
) : Clearable {
    private val baseCurrency get() = currencyManager.baseCurrency
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val dataExpiredSubject = PublishSubject.create<Unit>()
    val dataExpiredObservable: Observable<Unit> = dataExpiredSubject

    private val historicalRateSubject = PublishSubject.create<Pair<HistoricalRateKey, CurrencyValue>>()
    val historicalRateObservable: Observable<Pair<HistoricalRateKey, CurrencyValue>> = historicalRateSubject

    private val requestedXRates = mutableMapOf<HistoricalRateKey, Unit>()

    init {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                dataExpiredSubject.onNext(Unit)
            }
        }
    }

    fun getHistoricalRate(key: HistoricalRateKey): CurrencyValue? {
        return marketKit.coinHistoricalPrice(key.coinUid, baseCurrency.code, key.timestamp)?.let {
            CurrencyValue(baseCurrency, it)
        }
    }

    fun fetchHistoricalRate(key: HistoricalRateKey) {
        if (requestedXRates.containsKey(key)) return

        requestedXRates[key] = Unit

        coroutineScope.launch {
            try {
                val rate = marketKit.coinHistoricalPriceSingle(
                    key.coinUid,
                    baseCurrency.code,
                    key.timestamp
                ).await()

                if (rate.compareTo(BigDecimal.ZERO) != 0) {
                    historicalRateSubject.onNext(Pair(key, CurrencyValue(baseCurrency, rate)))
                }
            } catch (e: Throwable) {
                Log.w("XRate", "Could not fetch xrate for ${key.coinUid}:${key.timestamp}, ${e.javaClass.simpleName}:${e.message}")
            } finally {
                requestedXRates.remove(key)
            }
        }
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}

data class HistoricalRateKey(val coinUid: String, val timestamp: Long)
