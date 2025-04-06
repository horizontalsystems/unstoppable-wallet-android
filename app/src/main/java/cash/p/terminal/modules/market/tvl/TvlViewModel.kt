package cash.p.terminal.modules.market.tvl

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.ui_compose.components.ImageSource
import cash.p.terminal.modules.market.MarketModule
import cash.p.terminal.modules.market.tvl.TvlModule.SelectorDialogState
import cash.p.terminal.modules.market.tvl.TvlModule.TvlDiffType
import cash.p.terminal.ui.compose.Select
import io.horizontalsystems.core.models.HsTimePeriod
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class TvlViewModel(
    private val service: TvlService,
    private val tvlViewItemFactory: TvlViewItemFactory,
) : ViewModel() {

    private var tvlDiffType: TvlDiffType = TvlDiffType.Percent
        set(value) {
            field = value
            tvlDiffTypeLiveData.postValue(value)
        }
    private var tvlItems: List<TvlModule.MarketTvlItem> = listOf()

    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val tvlLiveData = MutableLiveData<TvlModule.TvlData>()
    val tvlDiffTypeLiveData = MutableLiveData(tvlDiffType)
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val chainSelectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    var header = MarketModule.Header(
        title = cash.p.terminal.strings.helpers.Translator.getString(R.string.MarketGlobalMetrics_TvlInDefi),
        description = cash.p.terminal.strings.helpers.Translator.getString(R.string.MarketGlobalMetrics_TvlInDefiDescription),
        icon = ImageSource.Remote("https://cdn.blocksdecoded.com/header-images/tvl@3x.png")
    )

    init {
        viewModelScope.launch {
            service.marketTvlItemsObservable.asFlow().collect { tvlItemsDataState ->
                tvlItemsDataState.viewState?.let {
                    viewStateLiveData.postValue(it)
                }

                tvlItemsDataState.dataOrNull?.let {
                    tvlItems = it
                    syncTvlItems(it)
                }
            }
        }

        service.start()
    }

    private fun syncTvlItems(tvlItems: List<TvlModule.MarketTvlItem>) {
        tvlLiveData.postValue(
            tvlViewItemFactory.tvlData(service.chain, service.chains, service.sortDescending, tvlItems)
        )
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
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
    }

    fun onSelectChartInterval(chartInterval: HsTimePeriod?) {
        service.updateChartInterval(chartInterval)
    }
}
