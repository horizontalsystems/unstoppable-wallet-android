package io.horizontalsystems.bankwallet.modules.chart

import androidx.annotation.CallSuper
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.chartview.ChartViewType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import java.util.Optional

abstract class AbstractChartService {
    open val hasVolumes = false
    abstract val chartIntervals: List<HsTimePeriod?>
    abstract val chartViewType: ChartViewType

    protected abstract val currencyManager: CurrencyManager
    protected abstract val initialChartInterval: HsTimePeriod
    protected open fun getAllItems(currency: Currency): Single<ChartPointsWrapper> {
        return Single.error(Exception("Not Implemented"))
    }
    protected abstract fun getItems(chartInterval: HsTimePeriod, currency: Currency): Single<ChartPointsWrapper>

    protected var chartInterval: HsTimePeriod? = null
        set(value) {
            field = value
            chartTypeObservable.onNext(Optional.ofNullable(value))
        }

    val currency: Currency
        get() = currencyManager.baseCurrency
    val chartTypeObservable = BehaviorSubject.create<Optional<HsTimePeriod>>()

    val chartPointsWrapperObservable = BehaviorSubject.create<Result<ChartPointsWrapper>>()

    protected val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var fetchItemsJob: Job? = null

    open suspend fun start() {
        coroutineScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
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

    fun refresh() {
        fetchItems()
    }

    @Synchronized
    private fun fetchItems() {
        fetchItemsJob?.cancel()
        fetchItemsJob = coroutineScope.launch {
            val tmpChartInterval = chartInterval
            val itemsSingle = when {
                tmpChartInterval == null -> getAllItems(currency)
                else -> getItems(tmpChartInterval, currency)
            }

            try {
                val chartPointsWrapper = itemsSingle.await()
                chartPointsWrapperObservable.onNext(Result.success(chartPointsWrapper))
            } catch (e: CancellationException) {
                // Do nothing
            } catch (e: Throwable) {
                chartPointsWrapperObservable.onNext(Result.failure(e))
            }
        }
    }
}

