package cash.p.terminal.core.stats

import android.util.Log
import com.google.gson.Gson
import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.storage.StatsDao
import cash.p.terminal.entities.StatRecord
import cash.p.terminal.modules.balance.BalanceSortType
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
    private var statsEnabled = false // TODO: P.CASH wallet will not collect and sent anonymous stats

    fun logStat(eventPage: StatPage, eventSection: StatSection? = null, event: StatEvent) {
        if (!statsEnabled) return

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
        if (!statsEnabled) return

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
