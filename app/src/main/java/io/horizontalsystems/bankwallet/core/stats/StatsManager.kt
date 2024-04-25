package cash.p.terminal.core.stats

import android.util.Log
import com.google.gson.Gson
import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.storage.StatsDao
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.StatRecord
import cash.p.terminal.modules.balance.BalanceSortType
import cash.p.terminal.modules.coin.CoinModule
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule
import cash.p.terminal.modules.market.MarketField
import cash.p.terminal.modules.market.SortingField
import cash.p.terminal.modules.metricchart.ProChartModule
import io.horizontalsystems.hdwalletkit.HDExtendedKey
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


val CoinModule.Tab.statTab: StatTab
    get() = when (this) {
        CoinModule.Tab.Overview -> StatTab.Overview
        CoinModule.Tab.Details -> StatTab.Analytics
        CoinModule.Tab.Market -> StatTab.Markets
    }

val CoinAnalyticsModule.RankType.statPage: StatPage
    get() = when (this) {
        CoinAnalyticsModule.RankType.CexVolumeRank -> StatPage.CoinRankCexVolume
        CoinAnalyticsModule.RankType.DexVolumeRank -> StatPage.CoinRankDexVolume
        CoinAnalyticsModule.RankType.DexLiquidityRank -> StatPage.CoinRankDexLiquidity
        CoinAnalyticsModule.RankType.AddressesRank -> StatPage.CoinRankAddress
        CoinAnalyticsModule.RankType.TransactionCountRank -> StatPage.CoinRankTxCount
        CoinAnalyticsModule.RankType.RevenueRank -> StatPage.CoinRankRevenue
        CoinAnalyticsModule.RankType.FeeRank -> StatPage.CoinRankFee
        CoinAnalyticsModule.RankType.HoldersRank -> StatPage.CoinRankHolders
    }

val AccountType.statAccountType: String
    get() = when(this) {
        is AccountType.Mnemonic -> {
            if (passphrase.isEmpty()) "mnemonic_${words.size}" else "mnemonic_with_passphrase_${words.size}"
        }
        is AccountType.BitcoinAddress -> {
            "btc_address"
        }
        is AccountType.Cex -> {
            "cex"
        }
        is AccountType.EvmAddress -> {
            "evm_address"
        }
        is AccountType.EvmPrivateKey -> {
            "evm_private_key"
        }
        is AccountType.HdExtendedKey -> {
            if (hdExtendedKey.isPublic) {
                "account_x_pub_key"
            } else {
                when(hdExtendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Bip32 -> "bip32"
                    HDExtendedKey.DerivedType.Master -> "bip32_root_key"
                    HDExtendedKey.DerivedType.Account -> "account_x_priv_key"
                }
            }
        }
        is AccountType.SolanaAddress -> {
            "sol_address"
        }
        is AccountType.TonAddress -> {
            "ton_address"
        }
        is AccountType.TronAddress -> {
            "tron_address"
        }
    }
