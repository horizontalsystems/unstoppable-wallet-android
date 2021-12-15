package io.horizontalsystems.bankwallet.modules.chart

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class ChartService(
    private val currencyManager: ICurrencyManager,
    private val chartRepo: IChartRepo,
) {

    private var chartType: ChartView.ChartType? = null
        set(value) {
            field = value
            value?.let { chartTypeObservable.onNext(it) }
        }
    val chartTypes by chartRepo::chartTypes
    val currency by currencyManager::baseCurrency
    val chartTypeObservable = BehaviorSubject.create<ChartView.ChartType>()

    val chartItemsObservable =
        BehaviorSubject.create<DataState<Pair<ChartView.ChartType, List<MetricChartModule.Item>>>>()

    private var fetchItemsDisposable: Disposable? = null
    private val disposables = CompositeDisposable()

    fun start() {
        chartRepo.dataUpdatedObservable
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

        chartType = chartRepo.initialChartType
        fetchItems()
    }

    fun stop() {
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
        fetchItemsDisposable = chartRepo.getItems(tmpChartType, currency)
            .doOnSubscribe {
                chartItemsObservable.onNext(DataState.Loading)
            }
            .subscribeIO({
                chartItemsObservable.onNext(DataState.Success(Pair(tmpChartType, it)))
            }, {
                chartItemsObservable.onNext(DataState.Error(it))
            })
    }

}