package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.AbstractChartService
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.Indicator
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

object MetricChartModule {

    data class Item(
        val value: BigDecimal,
        val dominance: BigDecimal?,
        val timestamp: Long,
        val volume: BigDecimal? = null,
        val indicators: Map<Indicator, Float?> = mapOf()
    )

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
        private val chartService by lazy {
            when (metricChartType) {
                MetricChartType.TradingVolume -> CoinTradingVolumeChartService(App.currencyManager, App.marketKit, coinUid, coinName)
                MetricChartType.Tvl -> CoinTvlChartService(App.currencyManager, App.marketKit, coinUid)
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                MetricChartViewModel::class.java -> {
                    val metricChartService = MetricChartService(chartService as IMetricChartFetcher)
                    MetricChartViewModel(metricChartService) as T
                }
                ChartViewModel::class.java -> {
                    ChartModule.createViewModel(chartService as AbstractChartService) as T
                }
                else -> throw IllegalArgumentException()
            }

        }
    }
}

data class SelectedPoint(val value: String, val date: String)
data class ChartViewItem(
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
