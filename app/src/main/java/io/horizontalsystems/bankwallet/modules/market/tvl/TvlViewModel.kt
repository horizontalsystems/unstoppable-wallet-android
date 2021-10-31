package io.horizontalsystems.bankwallet.modules.market.tvl

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.SelectorDialogState
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.TvlDiffType
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule.ViewState
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartModule.ValueType
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.chartview.ChartView
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TvlViewModel(
    private val service: TvlService,
    private val factory: MetricChartFactory,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private var tvlDiffType: TvlDiffType = TvlDiffType.Percent
    private var tvlItems: List<TvlModule.CoinTvlItem> = listOf()

    val loadingLiveData = MutableLiveData<Boolean>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val chartLiveData = MutableLiveData<TvlModule.ChartData>()
    val tvlLiveData = MutableLiveData<TvlModule.TvlData>()
    val viewStateLiveData = MutableLiveData<ViewState>()
    val chainSelectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    init {
        service.chartItemsObservable
            .subscribeIO { chartItemsDataState ->
                chartItemsDataState.dataOrNull?.let {
                    syncChartItems(it)
                }
            }
            .let { disposables.add(it) }

        service.coinTvlItemsObservable
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
                service.coinTvlItemsObservable,
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
                service.coinTvlItemsObservable
            )
        ) { it }.subscribeIO { array ->
            val viewState: ViewState? = when {
                array.any { it is DataState.Error } -> ViewState.Error
                array.all { it is DataState.Success<*> } -> ViewState.Success
                else -> null
            }
            viewState?.let {
                viewStateLiveData.postValue(it)
            }
        }.let { disposables.add(it) }


        service.start()
    }

    private fun syncTvlItems(tvlItems: List<TvlModule.CoinTvlItem>) {
        tvlLiveData.postValue(tvlData(tvlItems))
    }

    private fun syncChartItems(chartItems: List<MetricChartModule.Item>) {
        chartLiveData.postValue(chartData(chartItems))
    }

    private fun tvlData(tvlItems: List<TvlModule.CoinTvlItem>) =
        TvlModule.TvlData(
            TvlModule.Menu(
                chainSelect = Select(service.chain, service.chains),
                sortDescending = service.sortDescending,
                tvlDiffType = tvlDiffType
            ),
            tvlItems.map {
                coinTvlViewItem(it)
            })

    private fun chartData(chartItems: List<MetricChartModule.Item>): TvlModule.ChartData {
        val chartViewItem = factory.convert(
            chartItems,
            service.chartType,
            ValueType.CompactCurrencyValue,
            service.baseCurrency
        )
        val chartInfoData = ChartInfoData(
            chartViewItem.chartData,
            chartViewItem.chartType,
            chartViewItem.maxValue,
            chartViewItem.minValue
        )

        val diffValue = chartViewItem.lastValueWithDiff.diff
        val diff: TvlModule.Diff = diff(diffValue, suffix = "%")

        return TvlModule.ChartData(
            TvlModule.SubtitleViewItem(chartViewItem.lastValueWithDiff.value, diff),
            service.baseCurrency,
            chartInfoData
        )
    }

    private fun diff(diffValue: BigDecimal, prefix: String = "", suffix: String = ""): TvlModule.Diff {
        val diff: TvlModule.Diff = if (diffValue > BigDecimal.ZERO) {
            TvlModule.Diff.Positive(numberFormatter.format(diffValue.abs(), 0, 2, "+$prefix", suffix))
        } else {
            TvlModule.Diff.Negative(numberFormatter.format(diffValue.abs(), 0, 2, "-$prefix", suffix))
        }
        return diff
    }

    private fun coinTvlViewItem(item: TvlModule.CoinTvlItem) =
        TvlModule.CoinTvlViewItem(
            item.fullCoin,
            tvl = formatFiatShortened(item.tvl),
            tvlDiff = when (tvlDiffType) {
                TvlDiffType.Currency -> {
                    val (shortValue, suffix) = numberFormatter.shortenValue(item.diff.value.abs())
                    diff(
                        if (item.diff.value.signum() < 0) shortValue.negate() else shortValue,
                        prefix = item.diff.currency.symbol,
                        suffix = " $suffix"
                    )
                }
                TvlDiffType.Percent -> {
                    diff(item.diffPercent, suffix = "%")
                }
            },
            rank = item.rank
        )

    private fun formatFiatShortened(currencyValue: CurrencyValue): String {
        val (shortenValue, suffix) = numberFormatter.shortenValue(currencyValue.value)
        return numberFormatter.formatFiat(shortenValue, currencyValue.currency.symbol, 0, 2) + " $suffix"
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
        service.chartType = chartType
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
        syncTvlItems(tvlItems)
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
