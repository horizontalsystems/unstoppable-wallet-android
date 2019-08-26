package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.FeeRatePriority

data class FeeRateInfo(val priority: FeeRatePriority, val feeRate: Long, val duration: Long)
