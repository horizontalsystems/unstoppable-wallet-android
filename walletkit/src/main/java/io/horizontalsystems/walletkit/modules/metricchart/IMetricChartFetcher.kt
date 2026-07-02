package io.horizontalsystems.walletkit.modules.metricchart

import io.horizontalsystems.walletkit.ui.compose.TranslatableString

interface IMetricChartFetcher {
    val title: Int
    val description: TranslatableString
    val poweredBy: TranslatableString
}