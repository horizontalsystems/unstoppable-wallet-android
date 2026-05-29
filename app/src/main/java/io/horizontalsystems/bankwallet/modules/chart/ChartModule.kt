package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.SelectedItem
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.chartview.models.ChartVolumeType
import java.math.BigDecimal

object ChartModule {

    interface ChartNumberFormatter {
        fun formatValue(currency: Currency, value: BigDecimal, numberFormatter: IAppNumberFormatter): String
        fun formatMinMaxValue(currency: Currency, value: BigDecimal, numberFormatter: IAppNumberFormatter): String {
            return formatValue(currency, value, numberFormatter)
        }
    }

    data class ChartHeaderView(
        val value: String,
        val valueHint: String?,
        val date: String?,
        val diff: Value.Percent?,
        val extraData: ChartHeaderExtraData?
    )

    sealed class ChartHeaderExtraData {
        class Volume(val volume: String, val type: ChartVolumeType) : ChartHeaderExtraData()
        class Dominance(val dominance: String, val diff: Value.Percent?) : ChartHeaderExtraData()
        class Indicators(
            val movingAverages: List<SelectedItem.MA>,
            val rsi: Float?,
            val macd: SelectedItem.Macd?
        ) : ChartHeaderExtraData()
    }

}
