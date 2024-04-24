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
import io.horizontalsystems.bankwallet.modules.market.MarketField
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.modules.metricchart.ProChartModule
import io.horizontalsystems.marketkit.models.HsTimePeriod
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
    private val syncInterval = 0 //60 * 60 // 1H in seconds

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
                if (stats.isNotEmpty()) {
                    val statsArray = "[${stats.joinToString { it.json }}]"

                    Log.e("e", "send $statsArray")

                    marketKit.sendStats(statsArray, appConfigProvider.appVersion, appConfigProvider.appId).blockingGet()

                    statsDao.delete(stats.map { it.id })
                    localStorage.statsLastSyncTime = currentTime
                }

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

val ProChartModule.ChartType.statPage
    get() = when (this) {
        ProChartModule.ChartType.CexVolume -> StatPage.CoinAnalyticsCexVolume
        ProChartModule.ChartType.DexVolume -> StatPage.CoinAnalyticsDexVolume
        ProChartModule.ChartType.DexLiquidity -> StatPage.CoinAnalyticsDexLiquidity
        ProChartModule.ChartType.TxCount -> StatPage.CoinAnalyticsTxCount
        ProChartModule.ChartType.AddressesCount -> StatPage.CoinAnalyticsActiveAddresses
        ProChartModule.ChartType.Tvl -> StatPage.CoinAnalyticsTvl
    }

val HsTimePeriod?.statPeriod: StatPeriod
    get() = when (this) {
        HsTimePeriod.Day1 -> StatPeriod.Day1
        HsTimePeriod.Week1 -> StatPeriod.Week1
        HsTimePeriod.Week2 -> StatPeriod.Week2
        HsTimePeriod.Month1 -> StatPeriod.Month1
        HsTimePeriod.Month3 -> StatPeriod.Month3
        HsTimePeriod.Month6 -> StatPeriod.Month6
        HsTimePeriod.Year1 -> StatPeriod.Year1
        HsTimePeriod.Year2 -> StatPeriod.Year2
        HsTimePeriod.Year5 -> StatPeriod.Year5
        null -> StatPeriod.All
    }

val MarketField.statField: StatField
    get() = when (this) {
        MarketField.PriceDiff -> StatField.Price
        MarketField.MarketCap -> StatField.MarketCap
        MarketField.Volume -> StatField.Volume
    }

val SortingField.statSortType: StatSortType
    get() = when (this) {
        SortingField.HighestCap -> StatSortType.HighestCap
        SortingField.LowestCap -> StatSortType.LowestCap
        SortingField.HighestVolume -> StatSortType.HighestVolume
        SortingField.LowestVolume -> StatSortType.LowestVolume
        SortingField.TopGainers -> StatSortType.TopGainers
        SortingField.TopLosers -> StatSortType.TopLosers
    }