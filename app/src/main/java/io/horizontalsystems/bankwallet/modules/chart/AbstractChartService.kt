package io.horizontalsystems.bankwallet.modules.chart

import androidx.annotation.CallSuper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

abstract class AbstractChartService {
    abstract val chartIntervals: List<HsTimePeriod>
    open val chartIndicators: List<ChartIndicator> = listOf()

    protected abstract val currencyManager: ICurrencyManager
    protected abstract val initialChartInterval: HsTimePeriod
    protected abstract fun getItems(chartInterval: HsTimePeriod, currency: Currency): Single<ChartPointsWrapper>

    protected var chartInterval: HsTimePeriod? = null
        set(value) {
            field = value
            value?.let { chartTypeObservable.onNext(it) }
            indicatorsEnabled = chartInterval != HsTimePeriod.Day1
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
    val chartTypeObservable = BehaviorSubject.create<HsTimePeriod>()
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

        chartInterval = initialChartInterval
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
    open fun updateChartInterval(chartInterval: HsTimePeriod) {
        this.chartInterval = chartInterval

        fetchItems()
    }

    fun updateIndicator(indicator: ChartIndicator?) {
        this.indicator = indicator

        fetchItems()
    }

    @Synchronized
    private fun fetchItems() {
        val tmpChartInterval = chartInterval ?: return

        fetchItemsDisposable?.dispose()
        fetchItemsDisposable = getItems(tmpChartInterval, currency)
            .subscribeIO({
                chartPointsWrapperObservable.onNext(Result.success(it))
            }, {
                chartPointsWrapperObservable.onNext(Result.failure(it))
            })
    }

}

