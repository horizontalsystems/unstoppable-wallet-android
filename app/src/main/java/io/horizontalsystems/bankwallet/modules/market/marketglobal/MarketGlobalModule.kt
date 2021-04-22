package io.horizontalsystems.bankwallet.modules.market.marketglobal

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartView
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

object MarketGlobalModule {

    class Factory(private val metricsType: MetricsType) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketGlobalService(App.currencyManager.baseCurrency, App.xRateManager)
            return MarketGlobalViewModel(metricsType, service, App.numberFormatter, listOf(service)) as T
        }
    }

}

data class SelectedPoint(val value: String, val date: String)
data class LastValueWithDiff(val value: String, val diff: BigDecimal?)
data class ChartViewItem(
        val lastValueWithDiff: LastValueWithDiff?,
        val chartData: ChartData?,
        val maxValue: String?,
        val minValue: String?,
        val chartType: ChartView.ChartType,
        val loading: Boolean
        )

@Parcelize
enum class MetricsType: Parcelable{
    BtcDominance, Volume24h, DefiCap, TvlInDefi
}
