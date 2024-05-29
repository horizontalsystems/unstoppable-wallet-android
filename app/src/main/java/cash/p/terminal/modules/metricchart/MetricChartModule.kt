package cash.p.terminal.modules.metricchart

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class MetricsType : Parcelable {
    TotalMarketCap, Volume24h, Etf, TvlInDefi;
}
