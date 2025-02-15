package cash.p.terminal.modules.market.platform

import android.util.Log
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statPeriod
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import cash.p.terminal.modules.market.topplatforms.Platform
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.models.HsPeriodType
import io.horizontalsystems.core.models.HsTimePeriod
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import retrofit2.HttpException
import java.io.IOException

class PlatformChartService(
    private val platform: Platform,
    override val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper,
) : AbstractChartService() {

    override val initialChartInterval = HsTimePeriod.Week1
    override var chartIntervals = listOf<HsTimePeriod?>()
    override val chartViewType = ChartViewType.Line

    private var chartStartTime: Long = 0

    override suspend fun start() {
        try {
            chartStartTime = marketKit.topPlatformMarketCapStartTimeSingle(platform.uid).await()
        } catch (e: IOException) {
            Log.e("PlatformChartService", "start error: ", e)
        } catch (e: HttpException) {
            Log.e("PlatformChartService", "start error: ", e)
        }

        val now = System.currentTimeMillis() / 1000L
        val mostPeriodSeconds = now - chartStartTime

        chartIntervals = HsTimePeriod.values().filter {
            it.range <= mostPeriodSeconds
        } + listOf<HsTimePeriod?>(null)

        super.start()
    }

    override suspend fun getAllItems(currency: Currency): ChartPointsWrapper {
        return getChartPointsWrapper(currency, HsPeriodType.ByStartTime(chartStartTime)).await()
    }

    override suspend fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency,
    ): ChartPointsWrapper {
        return getChartPointsWrapper(currency, HsPeriodType.ByPeriod(chartInterval)).await()
    }

    override fun updateChartInterval(chartInterval: HsTimePeriod?) {
        super.updateChartInterval(chartInterval)

        stat(page = StatPage.TopPlatform, event = StatEvent.SwitchChartPeriod(chartInterval.statPeriod))
    }

    private fun getChartPointsWrapper(
        currency: Currency,
        periodType: HsPeriodType,
    ): Single<ChartPointsWrapper> {
        return try {
            marketKit.topPlatformMarketCapPointsSingle(platform.uid, currency.code, periodType)
                .map { info -> info.map { ChartPoint(it.marketCap.toFloat(), it.timestamp) } }
                .map { ChartPointsWrapper(it) }
        } catch (e: Exception) {
            Single.error(e)
        }
    }
}
