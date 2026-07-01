package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

interface IMetricChartFetcher {
    val title: Int
    val description: TranslatableString
    val poweredBy: TranslatableString
}