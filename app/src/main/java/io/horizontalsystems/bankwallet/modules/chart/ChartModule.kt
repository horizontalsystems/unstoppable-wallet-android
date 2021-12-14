package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory

object ChartModule {

    fun createViewModel(chartRepo: IChartRepo): ChartViewModel {
        val factory = MetricChartFactory(App.numberFormatter)
        val chartService = ChartService(App.currencyManager, chartRepo)
        return ChartViewModel(chartService, factory)
    }

}
