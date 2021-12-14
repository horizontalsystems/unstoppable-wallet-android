package io.horizontalsystems.bankwallet.modules.market.tvl

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.stringResId
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

interface XxxChartServiceRepo {
    val chartTypes: List<ChartView.ChartType>
    val dataUpdatedObservable: Observable<Unit>

    fun getItems(chartType: ChartView.ChartType, currency: Currency) : Single<List<MetricChartModule.Item>>
}

class XxxChartService(
    private val currencyManager: ICurrencyManager,
    private val repo: XxxChartServiceRepo,
) {

    private var chartType: ChartView.ChartType? = null
        set(value) {
            field = value
            value?.let { chartTypeObservable.onNext(it) }
        }
    val chartTypes by repo::chartTypes
    val currency by currencyManager::baseCurrency
    val chartTypeObservable = BehaviorSubject.create<ChartView.ChartType>()

    val chartItemsObservable = BehaviorSubject.create<DataState<Pair<ChartView.ChartType, List<MetricChartModule.Item>>>>()

    private var fetchItemsDisposable: Disposable? = null
    private val disposables = CompositeDisposable()

    fun start() {
        repo.dataUpdatedObservable
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

        chartType = chartTypes.firstOrNull()
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
        fetchItemsDisposable = repo.getItems(tmpChartType, currency)
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

class XxxChartViewModel(private val service: XxxChartService, private val factory: MetricChartFactory) : ViewModel() {
    val currentValueLiveData = MutableLiveData<String>()
    val currentValueDiffLiveData = MutableLiveData<Value.Percent>()
    val chartTabItemsLiveData = MutableLiveData<List<TabItem<ChartView.ChartType>>>()
    val chartInfoLiveData = MutableLiveData<ChartInfoData>()
    val chartLoadingLiveData = MutableLiveData<Boolean>()
    val chartViewStateLiveData = MutableLiveData<ViewState>()
    val currency by service::currency

    private val disposables = CompositeDisposable()

    init {
        service.chartTypeObservable
            .subscribeIO { chartType ->
                val tabItems = service.chartTypes.map {
                    TabItem(Translator.getString(it.stringResId), it == chartType, it)
                }
                chartTabItemsLiveData.postValue(tabItems)
            }
            .let {
                disposables.add(it)
            }

        service.chartItemsObservable
            .subscribeIO { chartItemsDataState ->
                chartViewStateLiveData.postValue(chartItemsDataState.viewState)

                chartLoadingLiveData.postValue(chartItemsDataState.loading)

                chartItemsDataState.dataOrNull?.let { (chartType, chartItems) ->
                    syncChartItems(chartType, chartItems)
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun onSelectChartType(chartType: ChartView.ChartType) {
        service.updateChartType(chartType)
    }

    private fun syncChartItems(
        chartType: ChartView.ChartType,
        chartItems: List<MetricChartModule.Item>,
    ) {
        chartItems.lastOrNull()?.let { lastItem ->
            val lastItemValue = lastItem.value
            val currentValue = App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, lastItemValue))
            currentValueLiveData.postValue(currentValue)

            val firstItemValue = chartItems.first().value
            val currentValueDiff = Value.Percent(((lastItemValue - firstItemValue).toFloat() / firstItemValue.toFloat() * 100).toBigDecimal())
            currentValueDiffLiveData.postValue(currentValueDiff)
        }

        val chartViewItem = factory.convert(
            chartItems,
            chartType,
            MetricChartModule.ValueType.CompactCurrencyValue,
            service.currency
        )
        val chartInfoData = ChartInfoData(
            chartViewItem.chartData,
            chartViewItem.chartType,
            chartViewItem.maxValue,
            chartViewItem.minValue
        )

        chartInfoLiveData.postValue(chartInfoData)
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }
}
