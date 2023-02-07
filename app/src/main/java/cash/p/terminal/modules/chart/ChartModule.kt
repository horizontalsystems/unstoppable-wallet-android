package cash.p.terminal.modules.chart

import cash.p.terminal.entities.Currency
import java.math.BigDecimal

object ChartModule {

    fun createViewModel(
        chartService: AbstractChartService,
        chartNumberFormatter: ChartNumberFormatter
    ): ChartViewModel {
        return ChartViewModel(chartService, chartNumberFormatter)
    }

    interface ChartNumberFormatter {
        fun formatValue(currency: Currency, value: BigDecimal): String
    }

}
