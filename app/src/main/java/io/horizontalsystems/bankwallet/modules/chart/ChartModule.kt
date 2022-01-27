package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.entities.CurrencyValue

object ChartModule {

    fun createViewModel(
        chartService: AbstractChartService,
        chartNumberFormatter: ChartNumberFormatter
    ): ChartViewModel {
        return ChartViewModel(chartService, chartNumberFormatter)
    }

    interface ChartNumberFormatter {
        fun formatValue(currencyValue: CurrencyValue): String
    }

}
