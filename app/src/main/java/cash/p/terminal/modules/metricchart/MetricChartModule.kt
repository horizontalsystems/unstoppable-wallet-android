package cash.p.terminal.modules.metricchart

import android.os.Parcelable
import cash.p.terminal.R
import cash.p.terminal.modules.market.ImageSource
import kotlinx.parcelize.Parcelize

@Parcelize
enum class MetricsType : Parcelable {
    TotalMarketCap, Volume24h, Etf, TvlInDefi;

    val title: Int
        get() = when (this) {
            TotalMarketCap -> R.string.MarketGlobalMetrics_TotalMarketCap
            Volume24h -> R.string.MarketGlobalMetrics_Volume
            Etf -> R.string.MarketGlobalMetrics_Etf
            TvlInDefi -> R.string.MarketGlobalMetrics_TvlInDefi
        }

    val description: Int
        get() = when (this) {
            TotalMarketCap -> R.string.MarketGlobalMetrics_TotalMarketCapDescription
            Volume24h -> R.string.MarketGlobalMetrics_VolumeDescription
            Etf -> R.string.MarketEtf_Description
            TvlInDefi -> R.string.MarketGlobalMetrics_TvlInDefiDescription
        }

    val headerIcon: ImageSource
        get() {
            val imageName = when (this) {
                TotalMarketCap,
                Volume24h -> "total_volume"
                Etf -> "defi_cap"
                TvlInDefi -> "tvl"
            }

            return ImageSource.Remote("https://cdn.blocksdecoded.com/header-images/$imageName@3x.png")
        }
}
