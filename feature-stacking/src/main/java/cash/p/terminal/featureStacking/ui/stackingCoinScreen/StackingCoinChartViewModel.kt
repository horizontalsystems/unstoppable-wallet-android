package cash.p.terminal.featureStacking.ui.stackingCoinScreen

import cash.p.terminal.featureStacking.ui.PirateInvestmentChartService
import io.horizontalsystems.chartview.chart.ChartCurrencyValueFormatterSignificant
import io.horizontalsystems.chartview.chart.ChartViewModel

abstract class StackingCoinChartViewModel(private val service: PirateInvestmentChartService) :
    ChartViewModel(
        service = service,
        valueFormatter = ChartCurrencyValueFormatterSignificant()
    ) {
    abstract val coinCode: String

    fun setReceiveAddress(address: String) {
        service.setData(coinCode = coinCode, address = address)
    }
}