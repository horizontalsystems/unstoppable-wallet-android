package cash.p.terminal.featureStacking.ui

import cash.p.terminal.network.domain.enity.InvestmentGraphData
import cash.p.terminal.network.domain.repository.PiratePlaceRepository
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.chart.AbstractChartService
import io.horizontalsystems.chartview.chart.ChartPointsWrapper
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.models.HsTimePeriod
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.rxSingle

class PirateInvestmentChartService(
    override val currencyManager: CurrencyManager,
    private val piratePlaceRepository: PiratePlaceRepository
) : AbstractChartService() {

    private var receiveAddress: String = ""

    fun setReceiveAddress(address: String) {
        receiveAddress = address
        refresh()
    }

    override val chartIntervals: List<HsTimePeriod> = listOf(
        HsTimePeriod.Day,
        HsTimePeriod.Week,
        HsTimePeriod.Month,
        HsTimePeriod.Year
    )

    override val initialChartInterval: HsTimePeriod = chartIntervals.first()

    override val chartViewType: ChartViewType = ChartViewType.Line

    override fun getItems(
        chartInterval: HsTimePeriod,
        currency: Currency
    ): Single<ChartPointsWrapper> {
        return if (receiveAddress.isEmpty()) {
            Single.never()
        } else {
            rxSingle(Dispatchers.IO) {
                mapToChartPointsWrapper(
                    piratePlaceRepository.getInvestmentChart(
                        receiveAddress,
                        chartInterval.value
                    )
                )
            }
        }
    }

    private fun mapToChartPointsWrapper(investmentGraphData: InvestmentGraphData): ChartPointsWrapper {
        val chartPoints = investmentGraphData.points.map {
            ChartPoint(
                value = it.balance.toFloat(),
                timestamp = it.from/1000,
            )
        }
        return ChartPointsWrapper(chartPoints)
    }
}