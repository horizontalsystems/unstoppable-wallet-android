package cash.p.terminal.wallet.models

data class IntervalData(
    val interval: HsPointTimePeriod,
    val fromTimestamp: Long?,
    val visibleTimestamp: Long,
)
