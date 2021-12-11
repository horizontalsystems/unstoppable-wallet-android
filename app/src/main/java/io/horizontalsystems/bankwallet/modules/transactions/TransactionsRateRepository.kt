package io.horizontalsystems.bankwallet.modules.transactions

import android.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

class TransactionsRateRepository(
    private val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit,
) : Clearable {
    private val baseCurrency get() = currencyManager.baseCurrency

    private val disposables = CompositeDisposable()

    private val dataExpiredSubject = PublishSubject.create<Unit>()
    val dataExpiredObservable: Observable<Unit> = dataExpiredSubject

    private val historicalRateSubject = PublishSubject.create<Pair<HistoricalRateKey, CurrencyValue>>()
    val historicalRateObservable: Observable<Pair<HistoricalRateKey, CurrencyValue>> = historicalRateSubject

    private val requestedXRates = mutableMapOf<HistoricalRateKey, Unit>()

    init {
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                dataExpiredSubject.onNext(Unit)
            }
            .let {
                disposables.add(it)
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

        marketKit.coinHistoricalPriceSingle(key.coinUid, baseCurrency.code, key.timestamp)
            .doFinally {
                requestedXRates.remove(key)
            }
            .subscribeIO({ rate ->
                if (rate.compareTo(BigDecimal.ZERO) != 0) {
                    historicalRateSubject.onNext(Pair(key, CurrencyValue(baseCurrency, rate)))
                }
            }, {
                Log.w("XRate", "Could not fetch xrate for ${key.coinUid}:${key.timestamp}, ${it.javaClass.simpleName}:${it.message}")
            })
            .let {
                disposables.add(it)
            }
    }

    override fun clear() {
        disposables.clear()
    }
}

data class HistoricalRateKey(val coinUid: String, val timestamp: Long)
