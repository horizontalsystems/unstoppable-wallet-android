package io.horizontalsystems.bankwallet.modules.transactions.q

import android.util.Log
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.concurrent.Executors

class TransactionsXRateRepository(
    private val currencyManager: ICurrencyManager,
    private val xRateManager: IRateManager,
) {
    val baseCurrency: Currency get() = currencyManager.baseCurrency

    private val itemsUpdatedSubject = PublishSubject.create<Unit>()
    val itemsUpdatedObservable: Observable<Unit> get() = itemsUpdatedSubject

    private val historicalRates = mutableMapOf<HistoricalRateKey, BigDecimal?>()
    private val disposables = CompositeDisposable()

    init {
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                handleUpdatedBaseCurrency()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun handleUpdatedBaseCurrency() {
        historicalRates.forEach { (key, _) ->
            historicalRates[key] = xRateManager.historicalRateCached(key.coinType, baseCurrency.code, key.timestamp)
        }
        itemsUpdatedSubject.onNext(Unit)
        xxx()
    }

    fun setRecords(transactionRecords: List<TransactionRecord>) {
        val historicalRateKeys = transactionRecords.mapNotNull { record ->
            record.mainValue?.let { mainValue ->
                HistoricalRateKey(mainValue.coin.type, record.timestamp)
            }
        }

        historicalRates.clear()
        historicalRateKeys.forEach {
            historicalRates[it] = xRateManager.historicalRateCached(it.coinType, baseCurrency.code, it.timestamp)
        }
    }

    fun getHistoricalRate(coinType: CoinType, timestamp: Long): BigDecimal? {
        return historicalRates[HistoricalRateKey(coinType, timestamp)]
    }

    private val executorService = Executors.newCachedThreadPool()

    fun xxx() {
        executorService.submit {
            historicalRates.filterValues { it == null }.map { (key, _) ->
                xRateManager.historicalRate(key.coinType, baseCurrency.code, key.timestamp)
                    .subscribeIO({ rate ->
                        historicalRates[key] = if (rate.compareTo(BigDecimal.ZERO) == 0) null else rate

                        itemsUpdatedSubject.onNext(Unit)
                    }, {
                        Log.e("AAA", "Could not fetch xrate for ${key.coinType}:${key.timestamp}, ${it.javaClass.simpleName}:${it.message}")
                    })
                    .let {
                        disposables.add(it)
                    }
            }
        }
    }


    data class HistoricalRateKey(val coinType: CoinType, val timestamp: Long)

}
