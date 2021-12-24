package io.horizontalsystems.bankwallet.modules.chart

object ChartModule {

    fun createViewModel(chartService: AbstractChartService): ChartViewModel {
        return ChartViewModel(chartService)
    }

}
