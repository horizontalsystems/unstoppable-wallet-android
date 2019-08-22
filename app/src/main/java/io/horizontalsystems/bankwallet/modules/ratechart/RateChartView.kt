package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.lib.chartview.ChartView
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import java.math.BigDecimal

class RateChartView : RateChartModule.View {
    val showSpinner = SingleLiveEvent<Unit>()
    val hideSpinner = SingleLiveEvent<Unit>()
    val setDefaultMode = SingleLiveEvent<ChartView.Mode>()
    val showRate = SingleLiveEvent<Pair<BigDecimal, BigDecimal>>()
    val showMarketCap = SingleLiveEvent<Triple<BigDecimal, BigDecimal, BigDecimal>>()
    val showChart = SingleLiveEvent<ChartData>()

    override fun showSpinner() {
        showSpinner.call()
    }

    override fun hideSpinner() {
        hideSpinner.call()
    }

    override fun setDefaultMode(mode: ChartView.Mode) {
        setDefaultMode.postValue(mode)
    }

    override fun showRate(rate: BigDecimal, startRate: BigDecimal) {
        showRate.postValue(Pair(rate, startRate))
    }

    override fun showMarketCap(value: BigDecimal, high: BigDecimal, low: BigDecimal) {
        showMarketCap.postValue(Triple(value, high, low))
    }

    override fun showChart(data: ChartData) {
        showChart.postValue(data)
    }
}
