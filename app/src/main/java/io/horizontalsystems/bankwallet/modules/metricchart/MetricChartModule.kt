package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartView
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

object MetricChartModule {

    interface IMetricChartConfiguration {
        val title: Int
        val description: Int?
        val valueType: ValueType
    }

    data class Item(val value: BigDecimal, val dominance: BigDecimal?, val timestamp: Long)

    enum class ValueType {
        Percent, CompactCurrencyValue, CurrencyValue
    }

    @Parcelize
    enum class MetricChartType : Parcelable {
        TradingVolume, Tvl
    }

    class Factory(
        private val coinUid: String,
        private val coinName: String,
        private val metricChartType: MetricChartType
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val fetcher = when (metricChartType) {
                MetricChartType.TradingVolume -> CoinTradingVolumeFetcher(App.marketKit, coinUid, coinName)
                MetricChartType.Tvl -> CoinTvlFetcher(App.marketKit, coinUid)
            }
            val metricChartService = MetricChartService(App.currencyManager.baseCurrency, fetcher)
            val factory = MetricChartFactory(App.numberFormatter)

            return MetricChartViewModel(metricChartService, factory) as T
        }
    }
}

data class SelectedPoint(val value: String, val date: String)
data class LastValueWithDiff(val value: String, val diff: BigDecimal)
data class ChartViewItem(
    val lastValueWithDiff: LastValueWithDiff,
    val chartData: ChartData,
    val maxValue: String?,
    val minValue: String?,
    val chartType: ChartView.ChartType
)

@Parcelize
enum class MetricsType : Parcelable {
    TotalMarketCap, BtcDominance, Volume24h, DefiCap, TvlInDefi;

    val title: Int
        get() = when (this) {
            TotalMarketCap -> R.string.MarketGlobalMetrics_TotalMarketCap
            BtcDominance -> R.string.MarketGlobalMetrics_BtcDominance
            Volume24h -> R.string.MarketGlobalMetrics_Volume
            DefiCap -> R.string.MarketGlobalMetrics_DefiCap
            TvlInDefi -> R.string.MarketGlobalMetrics_TvlInDefi
        }

    val description: Int
        get() = when (this) {
            TotalMarketCap -> R.string.MarketGlobalMetrics_TotalMarketCapDescription
            BtcDominance -> R.string.MarketGlobalMetrics_BtcDominanceDescription
            Volume24h -> R.string.MarketGlobalMetrics_VolumeDescription
            DefiCap -> R.string.MarketGlobalMetrics_DefiCapDescription
            TvlInDefi -> R.string.MarketGlobalMetrics_TvlInDefiDescription
        }
}
