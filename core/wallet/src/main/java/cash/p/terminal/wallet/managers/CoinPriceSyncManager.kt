package cash.p.terminal.wallet.managers

import cash.p.terminal.wallet.Scheduler
import cash.p.terminal.wallet.models.CoinPrice
import cash.p.terminal.wallet.providers.CoinPriceSchedulerFactory
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class CoinPriceKey(
    val tag: String,
    val coinUids: List<String>,
    val currencyCode: String
)

interface ICoinPriceCoinUidDataSource {
    fun allCoinUids(currencyCode: String): List<String>
    fun combinedCoinUids(currencyCode: String): Pair<List<String>, List<String>>
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

    private fun observingCoinUids(tag: String, currencyCode: String): Set<String> {
        return subjects
            .filter { it.key.tag == tag && it.key.currencyCode == currencyCode }
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
    override fun allCoinUids(currencyCode: String): List<String> {
        return observingCoinUids(currencyCode).toList()
    }

    override fun combinedCoinUids(currencyCode: String): Pair<List<String>, List<String>> {
        val allCoinUids = observingCoinUids(currencyCode).toList()
        val walletCoinUids = observingCoinUids("wallet", currencyCode).toList()
        return Pair(allCoinUids, walletCoinUids)
    }

    fun coinPriceObservable(tag: String, coinUid: String, currencyCode: String): Observable<CoinPrice> {
        val key = CoinPriceKey(tag, listOf(coinUid), currencyCode)

        return subject(key).flatMap { coinPriceMap ->
            coinPriceMap[coinUid]?.let { coinPrice ->
                Observable.just(coinPrice)
            } ?: Observable.never()
        }
    }

    fun coinPriceMapObservable(tag: String, coinUids: List<String>, currencyCode: String): Observable<Map<String, CoinPrice>> {
        val key = CoinPriceKey(tag, coinUids, currencyCode)
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
