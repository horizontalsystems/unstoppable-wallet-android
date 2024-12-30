package cash.p.terminal.wallet.models

import io.horizontalsystems.core.models.HsPeriodType

data class ChartInfoKey(
    val coinUid: String,
    val currencyCode: String,
    val periodType: HsPeriodType
)
