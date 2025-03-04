package cash.p.terminal.featureStacking.ui

import cash.p.terminal.network.data.entity.ChartPeriod
import cash.p.terminal.network.pirate.domain.enity.InvestmentGraphData
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.models.HsTimePeriod

class PirateInvestmentChartService(
    override val currencyManager: CurrencyManager,
    private val piratePlaceRepository: PiratePlaceRepository
) : AbstractChartService() {

    private var receiveAddress: String = ""
    private var coinCode: String = ""

    fun setData(coinCode: String, address: String) {
        this.coinCode = coinCode
        receiveAddress = address
        refresh()
    }

    override val chartIntervals: List<HsTimePeriod> = listOf(
        HsTimePeriod.Day1,
        HsTimePeriod.Week1,
        HsTimePeriod.Month1,
        HsTimePeriod.Year1
    )

    override val initialChartInterval: HsTimePeriod = chartIntervals.first()

    override val chartViewType: ChartViewType = ChartViewType.Line

    override suspend fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): ChartPointsWrapper {
        return if (receiveAddress.isEmpty()) {
            ChartPointsWrapper(emptyList())
        } else {
            val period = when (chartInterval) {
                HsTimePeriod.Day1 -> ChartPeriod.DAY
                HsTimePeriod.Week1 -> ChartPeriod.WEEK
                HsTimePeriod.Month1 -> ChartPeriod.MONTH
                HsTimePeriod.Year1 -> ChartPeriod.YEAR
                else -> ChartPeriod.DAY
            }
            mapToChartPointsWrapper(
                piratePlaceRepository.getInvestmentChart(
                    coin = coinCode,
                    address = receiveAddress,
                    period = period
                )
            )
        }
    }

    private fun mapToChartPointsWrapper(investmentGraphData: InvestmentGraphData): ChartPointsWrapper {
        val chartPoints = investmentGraphData.points.map {
            ChartPoint(
                value = it.balance.toFloat(),
                timestamp = it.from / 1000,
            )
        }
        return ChartPointsWrapper(chartPoints)
    }
}