package io.horizontalsystems.core.modules.metricchart

import io.horizontalsystems.core.ui.compose.TranslatableString

interface IMetricChartFetcher {
    val title: Int
    val description: TranslatableString
    val poweredBy: TranslatableString
}