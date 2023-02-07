package cash.p.terminal.entities

import cash.p.terminal.core.FeeRatePriority

data class FeeRateInfo(val priority: FeeRatePriority, var feeRate: Long, val duration: Long? = null)
