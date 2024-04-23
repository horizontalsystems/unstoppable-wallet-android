package io.horizontalsystems.bankwallet.core.stats

import android.util.Log
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.storage.StatsDao
import io.horizontalsystems.bankwallet.entities.StatRecord
import io.horizontalsystems.bankwallet.modules.balance.BalanceSortType
import java.time.Instant
import java.util.concurrent.Executors

fun stat(page: StatPage, section: StatSection? = null, event: StatEvent) {
    App.statsManager.logStat(page, section, event)
}

class StatsManager(
    private val statsDao: StatsDao,
    private val localStorage: ILocalStorage,
    private val marketKit: MarketKitWrapper,
    private val appConfigProvider: AppConfigProvider
) {
    private val gson by lazy { Gson() }
    private val executor = Executors.newCachedThreadPool()
    private val syncInterval = 60 * 60 // 1H in seconds

    fun logStat(eventPage: StatPage, eventSection: StatSection? = null, event: StatEvent) {
        executor.submit {
            try {
                val eventMap = buildMap {
                    put("event_page", eventPage.key)
                    put("event", event.name)
                    eventSection?.let { put("event_section", it.key) }
                    event.params?.let { params ->
                        putAll(params.map { (param, value) -> param.key to value })
                    }
                    put("time", Instant.now().epochSecond)
                }

                val json = gson.toJson(eventMap)
                Log.e("e", json)

                statsDao.insert(StatRecord(json))
            } catch (error: Throwable) {
                Log.e("e", "logStat error", error)
            }
        }
    }

    fun sendStats() {
        executor.submit {
            try {
                val statLastSyncTime = localStorage.statsLastSyncTime
                val currentTime = Instant.now().epochSecond

                if (currentTime - statLastSyncTime < syncInterval) return@submit

                val stats = statsDao.getAll()
                val statsArray = "[${stats.joinToString { it.json }}]"

                Log.e("e", "send $statsArray")
                marketKit.sendStats(statsArray, appConfigProvider.appVersion, appConfigProvider.appId).blockingGet()

                statsDao.delete(stats.map { it.id })
                localStorage.statsLastSyncTime = currentTime

            } catch (error: Throwable) {
                Log.e("e", "sendStats error", error)
            }
        }
    }

}

val BalanceSortType.statSortType
    get() = when (this) {
        BalanceSortType.Name -> StatSortType.Name
        BalanceSortType.PercentGrowth -> StatSortType.PriceChange
        BalanceSortType.Value -> StatSortType.Balance
    }
