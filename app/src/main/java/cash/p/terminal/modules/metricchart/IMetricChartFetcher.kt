package cash.p.terminal.modules.metricchart

import cash.p.terminal.strings.helpers.TranslatableString

interface IMetricChartFetcher {
    val title: Int
    val description: TranslatableString
    val poweredBy: TranslatableString
}