package cash.p.terminal.modules.transactions

import android.util.Log
import cash.p.terminal.wallet.Clearable
import io.horizontalsystems.core.CurrencyManager
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

class TransactionsRateRepository(
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
) : Clearable {
    private val baseCurrency get() = currencyManager.baseCurrency
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _dataExpiredFlow = MutableSharedFlow<Unit>()
    val dataExpiredFlow: SharedFlow<Unit> = _dataExpiredFlow.asSharedFlow()

    private val _historicalRateFlow = MutableSharedFlow<Pair<HistoricalRateKey, CurrencyValue>>()
    val historicalRateFlow: SharedFlow<Pair<HistoricalRateKey, CurrencyValue>> = _historicalRateFlow.asSharedFlow()

    private val requestedXRates = mutableMapOf<HistoricalRateKey, Unit>()

    init {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
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

                if (rate != null  && rate.compareTo(BigDecimal.ZERO) != 0) {
                    _historicalRateFlow.emit(Pair(key, CurrencyValue(baseCurrency, rate)))
                }
            } catch (e: Throwable) {
                Log.w(
                    "XRate",
                    "Could not fetch xrate for ${key.coinUid}:${key.timestamp}, ${e.javaClass.simpleName}:${e.message}"
                )
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
