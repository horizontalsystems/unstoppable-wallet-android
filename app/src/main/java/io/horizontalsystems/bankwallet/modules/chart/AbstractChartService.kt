package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

abstract class AbstractChartService {
    abstract val chartTypes: List<ChartView.ChartType>

    protected abstract val currencyManager: ICurrencyManager
    protected abstract val dataUpdatedObservable: Observable<Unit>
    protected abstract val initialChartType: ChartView.ChartType
    protected abstract fun getItems(chartType: ChartView.ChartType, currency: Currency): Single<ChartDataXxx>

    private var chartType: ChartView.ChartType? = null
        set(value) {
            field = value
            value?.let { chartTypeObservable.onNext(it) }
        }
    val currency: Currency
        get() = currencyManager.baseCurrency
    val chartTypeObservable = BehaviorSubject.create<ChartView.ChartType>()

    val chartDataXxxObservable =
        BehaviorSubject.create<DataState<ChartDataXxx>>()

    private var fetchItemsDisposable: Disposable? = null
    private val disposables = CompositeDisposable()

    fun start() {
        dataUpdatedObservable
            .subscribeIO {
                fetchItems()
            }
            .let {
                disposables.add(it)
            }

        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                fetchItems()
            }
            .let {
                disposables.add(it)
            }

        chartType = initialChartType
        fetchItems()
    }

    open fun stop() {
        disposables.clear()
        fetchItemsDisposable?.dispose()
    }

    fun updateChartType(chartType: ChartView.ChartType) {
        this.chartType = chartType

        fetchItems()
    }

    @Synchronized
    private fun fetchItems() {
        val tmpChartType = chartType ?: return

        fetchItemsDisposable?.dispose()
        fetchItemsDisposable = getItems(tmpChartType, currency)
            .doOnSubscribe {
                chartDataXxxObservable.onNext(DataState.Loading)
            }
            .subscribeIO({
                chartDataXxxObservable.onNext(DataState.Success(it))
            }, {
                chartDataXxxObservable.onNext(DataState.Error(it))
            })
    }

}

data class ChartDataXxx(
    val chartType: ChartView.ChartType,
    val items: List<MetricChartModule.Item>,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val isExpired: Boolean = false
) {
    constructor(chartType: ChartView.ChartType, items: List<MetricChartModule.Item>) : this(
        chartType,
        items,
        items.firstOrNull()?.timestamp ?: 0,
        items.lastOrNull()?.timestamp ?: 0,
    )
}