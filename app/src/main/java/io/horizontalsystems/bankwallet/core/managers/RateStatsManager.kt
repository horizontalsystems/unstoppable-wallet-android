package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.managers.ServiceExchangeApi.HostType
import io.horizontalsystems.bankwallet.entities.RateStatData
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable

class RateStatsManager(private val networkManager: INetworkManager) {

    private val disposables = CompositeDisposable()
    private val cache = mutableMapOf<StatsKey, RateStatData>()

    fun getRateStats(coinCode: String, currencyCode: String): Flowable<Pair<StatsKey, RateStatData>> {
        val statsKey = StatsKey(coinCode, currencyCode)
        val cached = cache[statsKey]
        if (cached != null) {
            return Flowable.just(Pair(statsKey, cached))
        }

        return networkManager.getRateStats(HostType.MAIN, coinCode, currencyCode)
                .onErrorResumeNext(networkManager.getRateStats(HostType.FALLBACK, coinCode, currencyCode))
                .map {
                    cache[statsKey] = it
                    Pair(statsKey, it)
                }
    }

    fun clear() {
        disposables.clear()
        cache.clear()
    }

    class StatsKey(val coinCode: String, val currencyCode: String)
}
