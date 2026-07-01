package io.horizontalsystems.bankwallet.modules.coin.treasuries

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.logoUrl
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesModule.CoinTreasuriesData
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesModule.CoinTreasuryItem
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesModule.SelectorDialogState
import io.horizontalsystems.bankwallet.modules.coin.treasuries.CoinTreasuriesModule.TreasuryTypeFilter
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.marketkit.models.CoinTreasury
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class CoinTreasuriesViewModel(
    private val service: CoinTreasuriesService,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val coinTreasuriesLiveData = MutableLiveData<CoinTreasuriesData>()
    val treasuryTypeSelectorDialogStateLiveData = MutableLiveData<SelectorDialogState>()

    init {
        viewModelScope.launch {
            service.stateObservable.asFlow()
                .catch {
                    viewStateLiveData.postValue(ViewState.Error(it))
                }
                .collect { state ->
                    handleServiceState(state)
                }
        }

        service.start()
    }

    private fun handleServiceState(state: DataState<List<CoinTreasury>>) {
        state.dataOrNull?.let {
            viewStateLiveData.postValue(ViewState.Success)
            syncCoinTreasuriesData(it)
        }

        state.errorOrNull?.let {
            viewStateLiveData.postValue(ViewState.Error(it))
        }
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
