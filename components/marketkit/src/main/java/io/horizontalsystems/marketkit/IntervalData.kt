package io.horizontalsystems.marketkit

import io.horizontalsystems.marketkit.models.HsPointTimePeriod

data class IntervalData(
    val interval: HsPointTimePeriod,
    val fromTimestamp: Long?,
    val visibleTimestamp: Long,
)
