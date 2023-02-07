package cash.p.terminal.modules.metricchart

import cash.p.terminal.ui.compose.TranslatableString

interface IMetricChartFetcher {
    val title: Int
    val description: TranslatableString
    val poweredBy: TranslatableString
}