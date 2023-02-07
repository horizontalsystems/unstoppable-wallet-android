package cash.p.terminal.modules.coin.treasuries

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.IAppNumberFormatter
import cash.p.terminal.core.logoUrl
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.DataState
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.treasuries.CoinTreasuriesModule.CoinTreasuriesData
import cash.p.terminal.modules.coin.treasuries.CoinTreasuriesModule.CoinTreasuryItem
import cash.p.terminal.modules.coin.treasuries.CoinTreasuriesModule.SelectorDialogState
import cash.p.terminal.modules.coin.treasuries.CoinTreasuriesModule.TreasuryTypeFilter
import cash.p.terminal.ui.compose.Select
import io.horizontalsystems.marketkit.models.CoinTreasury
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoinTreasuriesViewModel(
    private val service: CoinTreasuriesService,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {
    private val disposables = CompositeDisposable()

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val coinTreasuriesLiveData = MutableLiveData<CoinTreasuriesData>()
    val treasuryTypeSelectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    init {
        service.stateObservable
            .subscribeIO({ state ->
                when (state) {
                    is DataState.Success -> {
                        viewStateLiveData.postValue(ViewState.Success)

                        syncCoinTreasuriesData(state.data)
                    }
                    is DataState.Error -> {
                        viewStateLiveData.postValue(ViewState.Error(state.error))
                    }
                    DataState.Loading -> {}
                }
            }, {
                viewStateLiveData.postValue(ViewState.Error(it))
            }).let {
                disposables.add(it)
            }

        service.start()
    }

    fun refresh() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onErrorClick() {
        refreshWithMinLoadingSpinnerPeriod()
    }

    fun onToggleSortType() {
        service.sortDescending = !service.sortDescending
    }

    fun onClickTreasuryTypeSelector() {
        treasuryTypeSelectorDialogStateLiveData.postValue(
            SelectorDialogState.Opened(Select(service.treasuryType, service.treasuryTypes))
        )
    }

    fun onSelectTreasuryType(type: TreasuryTypeFilter) {
        service.treasuryType = type
        treasuryTypeSelectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    fun onTreasuryTypeSelectorDialogDismiss() {
        treasuryTypeSelectorDialogStateLiveData.postValue(SelectorDialogState.Closed)
    }

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    private fun refreshWithMinLoadingSpinnerPeriod() {
        service.refresh()
        viewModelScope.launch {
            isRefreshingLiveData.postValue(true)
            delay(1000)
            isRefreshingLiveData.postValue(false)
        }
    }

    private fun syncCoinTreasuriesData(coinTreasuries: List<CoinTreasury>) {
        val coinTreasuriesData = CoinTreasuriesData(
            Select(service.treasuryType, service.treasuryTypes),
            service.sortDescending,
            coinTreasuries.map {
                coinTreasuryItem(it)
            }
        )
        coinTreasuriesLiveData.postValue(coinTreasuriesData)
    }

    private fun coinTreasuryItem(coinTreasury: CoinTreasury) =
        CoinTreasuryItem(
            fund = coinTreasury.fund,
            fundLogoUrl = coinTreasury.logoUrl,
            country = coinTreasury.countryCode,
            amount = numberFormatter.formatCoinShort(coinTreasury.amount, service.coin.code, 8),
            amountInCurrency = numberFormatter.formatFiatShort(
                coinTreasury.amountInCurrency,
                service.currency.symbol,
                2
            )
        )
}
