package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory

object ChartModule {

    fun createViewModel(chartService: AbstractChartService): ChartViewModel {
        val factory = MetricChartFactory(App.numberFormatter)
        return ChartViewModel(chartService, factory)
    }

}
