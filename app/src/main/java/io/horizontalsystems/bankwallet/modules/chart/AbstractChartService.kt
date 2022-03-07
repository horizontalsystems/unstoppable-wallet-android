package io.horizontalsystems.bankwallet.modules.chart

import androidx.annotation.CallSuper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

abstract class AbstractChartService {
    abstract val chartTypes: List<ChartView.ChartType>
    open val chartIndicators: List<ChartIndicator> = listOf()

    protected abstract val currencyManager: ICurrencyManager
    protected abstract val initialChartType: ChartView.ChartType
    protected abstract fun getItems(chartType: ChartView.ChartType, currency: Currency): Single<ChartPointsWrapper>

    protected var chartType: ChartView.ChartType? = null
        set(value) {
            field = value
            value?.let { chartTypeObservable.onNext(it) }
            indicatorsEnabled = chartType != ChartView.ChartType.TODAY && chartType != ChartView.ChartType.DAILY
        }
    var indicator: ChartIndicator? = null
        private set(value) {
            field = value
            indicatorObservable.onNext(Optional.ofNullable(value))
        }
    private var indicatorsEnabled = true
        set(value) {
            field = value
            indicatorsEnabledObservable.onNext(value)
        }

    val currency: Currency
        get() = currencyManager.baseCurrency
    val chartTypeObservable = BehaviorSubject.create<ChartView.ChartType>()
    val indicatorObservable = BehaviorSubject.create<Optional<ChartIndicator>>()

    val indicatorsEnabledObservable = BehaviorSubject.create<Boolean>()

    val chartPointsWrapperObservable = BehaviorSubject.create<Result<ChartPointsWrapper>>()

    private var fetchItemsDisposable: Disposable? = null
    private val disposables = CompositeDisposable()

    fun start() {
        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                fetchItems()
            }
            .let {
                disposables.add(it)
            }

        chartType = initialChartType
        indicator = null
        fetchItems()
    }

    protected fun dataInvalidated() {
        fetchItems()
    }

    open fun stop() {
        disposables.clear()
        fetchItemsDisposable?.dispose()
    }

    @CallSuper
    open fun updateChartType(chartType: ChartView.ChartType) {
        this.chartType = chartType

        fetchItems()
    }

    fun updateIndicator(indicator: ChartIndicator?) {
        this.indicator = indicator

        fetchItems()
    }

    @Synchronized
    private fun fetchItems() {
        val tmpChartType = chartType ?: return

        fetchItemsDisposable?.dispose()
        fetchItemsDisposable = getItems(tmpChartType, currency)
            .subscribeIO({
                chartPointsWrapperObservable.onNext(Result.success(it))
            }, {
                chartPointsWrapperObservable.onNext(Result.failure(it))
            })
    }

}

