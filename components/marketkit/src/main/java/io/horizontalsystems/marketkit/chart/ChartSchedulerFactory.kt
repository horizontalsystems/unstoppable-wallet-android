package io.horizontalsystems.marketkit.chart

import io.horizontalsystems.marketkit.models.ChartInfoKey
import io.horizontalsystems.marketkit.chart.scheduler.ChartScheduler
import io.horizontalsystems.marketkit.providers.HsProvider

class ChartSchedulerFactory(
    private val manager: ChartManager,
    private val provider: HsProvider,
    private val indicatorPoints: Int,
    private val retryInterval: Long = 30
) {

    fun getScheduler(key: ChartInfoKey): ChartScheduler {
        return ChartScheduler(ChartSchedulerProvider(retryInterval, key, provider, manager, indicatorPoints))
    }
}
