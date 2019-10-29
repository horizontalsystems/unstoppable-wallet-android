package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.xrateskit.entities.ChartType

class RateChartView : RateChartModule.View {
    val showSpinner = SingleLiveEvent<Unit>()
    val hideSpinner = SingleLiveEvent<Unit>()
    val setDefaultMode = SingleLiveEvent<ChartType>()
    val setSelectedPoint = SingleLiveEvent<Triple<Long, CurrencyValue, ChartType>>()
    val showChart = SingleLiveEvent<ChartViewItem>()
    val showError = SingleLiveEvent<Throwable>()

    override fun showSpinner() {
        showSpinner.call()
    }

    override fun hideSpinner() {
        hideSpinner.call()
    }

    override fun setChartType(type: ChartType) {
        setDefaultMode.postValue(type)
    }

    override fun showChart(viewItem: ChartViewItem) {
        showChart.postValue(viewItem)
    }

    override fun showSelectedPoint(data: Triple<Long, CurrencyValue, ChartType>) {
        setSelectedPoint.postValue(data)
    }

    override fun showError(ex: Throwable) {
        showError.postValue(ex)
    }
}
