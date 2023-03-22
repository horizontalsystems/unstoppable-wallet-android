package io.horizontalsystems.bankwallet.modules.metricchart

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import kotlinx.parcelize.Parcelize

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

    val headerIcon: ImageSource
        get() {
            val imageName = when (this) {
                TotalMarketCap,
                BtcDominance -> "total_mcap"
                Volume24h -> "total_volume"
                DefiCap -> "defi_cap"
                TvlInDefi -> "tvl"
            }

            return ImageSource.Remote("https://cdn.blocksdecoded.com/header-images/$imageName@3x.png")
        }
}
