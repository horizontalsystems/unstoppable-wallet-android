package io.horizontalsystems.walletkit.modules.transactions

import android.util.Log
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.entities.CurrencyValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TransactionsRateRepository(
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
) : Clearable {
    private val baseCurrency get() = currencyManager.baseCurrency
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _dataExpiredFlow = MutableSharedFlow<Unit>()
    val dataExpiredObservable: SharedFlow<Unit> = _dataExpiredFlow.asSharedFlow()

    private val _historicalRateFlow = MutableSharedFlow<Pair<HistoricalRateKey, CurrencyValue>>()
    val historicalRateObservable: SharedFlow<Pair<HistoricalRateKey, CurrencyValue>> = _historicalRateFlow.asSharedFlow()

    private val requestedXRates = mutableMapOf<HistoricalRateKey, Unit>()

    init {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                _dataExpiredFlow.emit(Unit)
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
                )

                if (rate != null && rate.compareTo(BigDecimal.ZERO) != 0) {
                    _historicalRateFlow.emit(Pair(key, CurrencyValue(baseCurrency, rate)))
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
