package io.horizontalsystems.bankwallet.modules.chart

import android.os.Parcelable
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.market.Value
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

object ChartModule {

    fun createViewModel(
        chartService: AbstractChartService,
        chartNumberFormatter: ChartNumberFormatter,
        overriddenValue: OverriddenValue? = null
    ): ChartViewModel {
        return ChartViewModel(chartService, chartNumberFormatter, overriddenValue)
    }

    interface ChartNumberFormatter {
        fun formatValue(currency: Currency, value: BigDecimal): String
    }

    data class ChartHeaderView(
        val value: String,
        val valueHint: String?,
        val date: String?,
        val diff: Value.Percent?,
        val extraData: ChartHeaderExtraData?
    )

    sealed class ChartHeaderExtraData {
        class Volume(val volume: String) : ChartHeaderExtraData()
        class Dominance(val dominance: String, val diff: Value.Percent?) : ChartHeaderExtraData()
    }

    @Parcelize
    data class OverriddenValue(
        val value: String,
        val description: String?
    ): Parcelable

}
