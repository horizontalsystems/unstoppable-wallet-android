package com.quantum.wallet.bankwallet.modules.metricchart

import com.quantum.wallet.bankwallet.ui.compose.TranslatableString

interface IMetricChartFetcher {
    val title: Int
    val description: TranslatableString
    val poweredBy: TranslatableString
}