package io.horizontalsystems.bankwallet.modules.market.tvl

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.SelectorDialogState
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.TvlDiffType
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule.ValueType
import io.horizontalsystems.bankwallet.modules.metricchart.stringResId
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.chartview.ChartView
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TvlViewModel(
    private val service: TvlService,
    private val factory: MetricChartFactory,
    private val tvlViewItemFactory: TvlViewItemFactory
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private var tvlDiffType: TvlDiffType = TvlDiffType.Percent
        set(value) {
            field = value
            tvlDiffTypeLiveData.postValue(value)
        }
    private var tvlItems: List<TvlModule.MarketTvlItem> = listOf()

    val loadingLiveData = MutableLiveData<Boolean>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val tvlLiveData = MutableLiveData<TvlModule.TvlData>()
    val tvlDiffTypeLiveData = MutableLiveData(tvlDiffType)
    val viewStateLiveData = MutableLiveData<ViewState>()
    val chainSelectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    val currentValueLiveData = MutableLiveData<String>()
    val currentValueDiffLiveData = MutableLiveData<Value.Percent>()
    val chartTabItemsLiveData = MutableLiveData<List<TabItem<ChartView.ChartType>>>()
    val chartInfoLiveData = MutableLiveData<ChartInfoData>()
    val chartLoadingLiveData = MutableLiveData<Boolean>()
    val chartViewStateLiveData = MutableLiveData<ViewState>()
    val currency by service::currency

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
            .let { disposables.add(it) }

        service.marketTvlItemsObservable
            .subscribeIO { tvlItemsDataState ->
                tvlItemsDataState.dataOrNull?.let {
                    tvlItems = it
                    syncTvlItems(it)
                }
            }
            .let { disposables.add(it) }

        Observable.combineLatest(
            listOf(
                service.chartItemsObservable,
                service.marketTvlItemsObservable,
            )
        ) { array -> array.map { it is DataState.Loading } }
            .map { loadingArray ->
                loadingArray.any { it }
            }
            .subscribeIO { loading ->
                loadingLiveData.postValue(loading)
            }
            .let { disposables.add(it) }

        Observable.combineLatest(
            listOf(
                service.chartItemsObservable,
                service.marketTvlItemsObservable
            )
        ) { it }.subscribeIO { array ->
            val viewState: ViewState? = when {
                array.any { it is DataState.Error } -> ViewState.Error(array.filterIsInstance<DataState.Error>().first().error)
                array.all { it is DataState.Success<*> } -> ViewState.Success
                else -> null
            }
            viewState?.let {
                viewStateLiveData.postValue(it)
            }
        }.let { disposables.add(it) }


        service.start()
    }

    private fun syncTvlItems(tvlItems: List<TvlModule.MarketTvlItem>) {
        tvlLiveData.postValue(
            tvlViewItemFactory.tvlData(service.chain, service.chains, service.sortDescending, tvlItems)
        )
    }

    private fun syncChartItems(chartType: ChartView.ChartType, chartItems: List<MetricChartModule.Item>) {
        chartItems.lastOrNull()?.let { lastItem ->
            val lastItemValue = lastItem.value
            currentValueLiveData.postValue(
                App.numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, lastItemValue))
            )

            val firstItemValue = chartItems.first().value
            currentValueDiffLiveData.postValue(Value.Percent(((lastItemValue - firstItemValue).toFloat() / firstItemValue.toFloat() * 100).toBigDecimal()))
        }

        val chartViewItem = factory.convert(
            chartItems,
            chartType,
            ValueType.CompactCurrencyValue,
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

    private fun refreshWithMinLoadingSpinnerPeriod() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
    }

    fun onSelectChartType(chartType: ChartView.ChartType) {
        service.updateChartType(chartType)
    }

    fun onSelectChain(chain: TvlModule.Chain) {
        service.chain = chain
        chainSelectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    fun onToggleSortType() {
        service.sortDescending = !service.sortDescending
    }

    fun onToggleTvlDiffType() {
        tvlDiffType = if (tvlDiffType == TvlDiffType.Percent) TvlDiffType.Currency else TvlDiffType.Percent
    }

    fun onClickChainSelector() {
        chainSelectorDialogStateLiveData.postValue(
            SelectorDialogState.Opened(Select(service.chain, service.chains))
        )
    }

    fun onChainSelectorDialogDismiss() {
        chainSelectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }
}
