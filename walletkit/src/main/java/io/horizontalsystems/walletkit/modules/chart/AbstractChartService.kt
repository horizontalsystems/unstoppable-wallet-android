package io.horizontalsystems.walletkit.modules.chart

import androidx.annotation.CallSuper
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.marketkit.models.HsTimePeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Optional

abstract class AbstractChartService {
    open val hasVolumes = false
    abstract val chartIntervals: List<HsTimePeriod?>
    abstract val chartViewType: ChartViewType

    protected abstract val currencyManager: CurrencyManager
    protected abstract val initialChartInterval: HsTimePeriod
    protected open suspend fun getAllItems(currency: Currency): ChartPointsWrapper {
        throw Exception("Not Implemented")
    }
    protected abstract suspend fun getItems(chartInterval: HsTimePeriod, currency: Currency): ChartPointsWrapper

    protected var chartInterval: HsTimePeriod? = null
        set(value) {
            field = value
            _chartTypeFlow.tryEmit(Optional.ofNullable(value))
        }

    val currency: Currency
        get() = currencyManager.baseCurrency

    private val _chartTypeFlow = MutableSharedFlow<Optional<HsTimePeriod>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val chartTypeFlow: SharedFlow<Optional<HsTimePeriod>> = _chartTypeFlow.asSharedFlow()

    private val _chartPointsWrapperFlow = MutableSharedFlow<Result<ChartPointsWrapper>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val chartPointsWrapperFlow: SharedFlow<Result<ChartPointsWrapper>> = _chartPointsWrapperFlow.asSharedFlow()

    protected val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var fetchItemsJob: Job? = null

    open suspend fun start() {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedFlow.collect {
                fetchItems()
            }
        }

        chartInterval = initialChartInterval
        fetchItems()
    }

    protected fun dataInvalidated() {
        fetchItems()
    }

    open fun stop() {
        coroutineScope.cancel()
    }

    @CallSuper
    open fun updateChartInterval(chartInterval: HsTimePeriod?) {
        this.chartInterval = chartInterval

        fetchItems()
    }

    open fun chartPointsDiff(items: List<ChartPoint>): BigDecimal {
        val values = items.map { it.value }
        if (values.isEmpty()) {
            return BigDecimal.ZERO
        }

        val firstValue = values.find { it != 0f }
        val lastValue = values.last()
        if (lastValue == 0f || firstValue == null) {
            return BigDecimal.ZERO
        }

        return try {
            ((lastValue - firstValue) / firstValue * 100).toBigDecimal()
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    fun refresh() {
        fetchItems()
    }

    @Synchronized
    private fun fetchItems() {
        fetchItemsJob?.cancel()
        fetchItemsJob = coroutineScope.launch {
            val tmpChartInterval = chartInterval

            try {
                val chartPointsWrapper = when {
                    tmpChartInterval == null -> getAllItems(currency)
                    else -> getItems(tmpChartInterval, currency)
                }
                _chartPointsWrapperFlow.tryEmit(Result.success(chartPointsWrapper))
            } catch (e: CancellationException) {
                // Do nothing
            } catch (e: Throwable) {
                _chartPointsWrapperFlow.tryEmit(Result.failure(e))
            }
        }
    }
}
