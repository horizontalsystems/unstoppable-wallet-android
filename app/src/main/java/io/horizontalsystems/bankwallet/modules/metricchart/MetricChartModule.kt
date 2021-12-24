package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import kotlinx.android.parcel.Parcelize

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
