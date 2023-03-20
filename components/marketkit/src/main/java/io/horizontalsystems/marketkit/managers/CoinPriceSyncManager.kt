package io.horizontalsystems.marketkit.managers

import io.horizontalsystems.marketkit.models.CoinPrice
import io.horizontalsystems.marketkit.providers.CoinPriceSchedulerFactory
import io.horizontalsystems.marketkit.Scheduler
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class CoinPriceKey(
    val coinUids: List<String>,
    val currencyCode: String
)

interface ICoinPriceCoinUidDataSource {
    fun coinUids(currencyCode: String): List<String>
}

class CoinPriceSyncManager(
    private val schedulerFactory: CoinPriceSchedulerFactory
) : CoinPriceManager.Listener, ICoinPriceCoinUidDataSource {

    private val schedulers = ConcurrentHashMap<String, Scheduler>()
    private val subjects = ConcurrentHashMap<CoinPriceKey, PublishSubject<Map<String, CoinPrice>>>()
    private val observers = ConcurrentHashMap<CoinPriceKey, AtomicInteger>()

    private fun observingCoinUids(currencyCode: String): Set<String> {
        return subjects
            .filter { it.key.currencyCode == currencyCode }
            .map { it.key.coinUids }
            .flatten()
            .toSet()
    }


    private fun needForceUpdate(key: CoinPriceKey): Boolean {
        //get set of all listening coins
        //found tokens which needed to update
        //make new key for force update

        val newCoinTypes = key.coinUids.minus(observingCoinUids(key.currencyCode))
        return newCoinTypes.isNotEmpty()
    }

    private fun getCounter(key: CoinPriceKey): AtomicInteger {
        var count = observers[key]
        if (count == null) {
            count = AtomicInteger(0)
            observers[key] = count
        }

        return count
    }

    private fun cleanUp(key: CoinPriceKey) {
        val subject = subjects[key] ?: return
        if (getCounter(key).get() > 0) return

        subject.onComplete()
        subjects.remove(key)

        if (subjects.none { it.key.currencyCode == key.currencyCode }) {
            schedulers[key.currencyCode]?.stop()
            schedulers.remove(key.currencyCode)
        }
    }

    private fun subject(key: CoinPriceKey): Observable<Map<String, CoinPrice>> {
        val subject: PublishSubject<Map<String, CoinPrice>>
        var forceUpdate = false

        val candidate = subjects[key]
        if (candidate != null) {
            subject = candidate
        } else {                                        // create new subject
            forceUpdate = needForceUpdate(key)     // if subject has non-subscribed tokens we need force schedule

            subject = PublishSubject.create()
            subjects[key] = subject
        }

        if (schedulers[key.currencyCode] == null) {        // create scheduler if not exist
            val scheduler = schedulerFactory.scheduler(key.currencyCode, this)

            schedulers[key.currencyCode] = scheduler
        }

        if (forceUpdate) {                                // make request for scheduler immediately
            schedulers[key.currencyCode]?.start(true)
        }

        return subject
            .doOnSubscribe {
                getCounter(key).incrementAndGet()
            }
            .doOnDispose {
                getCounter(key).decrementAndGet()
                cleanUp(key)
            }
            .doOnError {
                getCounter(key).decrementAndGet()
                cleanUp(key)
            }
    }

    // ICoinPriceCoinUidDataSource
    override fun coinUids(currencyCode: String): List<String> {
        return observingCoinUids(currencyCode).toList()
    }

    fun coinPriceObservable(coinUid: String, currencyCode: String): Observable<CoinPrice> {
        val key = CoinPriceKey(listOf(coinUid), currencyCode)

        return subject(key).flatMap { coinPriceMap ->
            coinPriceMap[coinUid]?.let { coinPrice ->
                Observable.just(coinPrice)
            } ?: Observable.never()
        }
    }

    fun coinPriceMapObservable(coinUids: List<String>, currencyCode: String): Observable<Map<String, CoinPrice>> {
        val key = CoinPriceKey(coinUids, currencyCode)
        return subject(key)
    }

    fun refresh(currencyCode: String) {
        schedulers[currencyCode]?.start(force = true)
    }

    //  CoinPriceManager.Listener

    override fun didUpdate(coinPriceMap: Map<String, CoinPrice>, currencyCode: String) {
        subjects.forEach { (key, subject) ->
            if (key.currencyCode == currencyCode) {
                val rates = coinPriceMap.filter { (coinUid, _) ->
                    key.coinUids.contains(coinUid)
                }
                if (rates.isNotEmpty()) {
                    subject.onNext(rates)
                }
            }
        }
    }
}
