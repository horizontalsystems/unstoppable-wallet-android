package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.FeeRatePriority

data class FeeRateInfo(val priority: FeeRatePriority, var feeRate: Long, val duration: Long? = null)
