package io.horizontalsystems.core.modules.metricchart

import kotlinx.serialization.Serializable

@Serializable
enum class MetricsType {
    TotalMarketCap, Volume24h, Etf, TvlInDefi;
}
