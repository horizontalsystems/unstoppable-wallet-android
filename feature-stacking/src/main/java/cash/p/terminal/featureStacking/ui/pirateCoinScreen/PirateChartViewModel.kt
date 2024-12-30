package cash.p.terminal.featureStacking.ui.pirateCoinScreen

import cash.p.terminal.featureStacking.ui.PirateInvestmentChartService
import io.horizontalsystems.chartview.chart.ChartCurrencyValueFormatterSignificant
import io.horizontalsystems.chartview.chart.ChartViewModel

class PirateChartViewModel(private val service: PirateInvestmentChartService) : ChartViewModel(
    service = service,
    valueFormatter = ChartCurrencyValueFormatterSignificant()
) {
    fun setReceiveAddress(address: String) = service.setReceiveAddress(address)
}