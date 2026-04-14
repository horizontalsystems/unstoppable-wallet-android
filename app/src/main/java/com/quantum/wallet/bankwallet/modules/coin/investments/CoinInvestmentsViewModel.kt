package com.quantum.wallet.bankwallet.modules.coin.investments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.IAppNumberFormatter
import com.quantum.wallet.bankwallet.core.logoUrl
import com.quantum.wallet.bankwallet.entities.DataState
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.investments.CoinInvestmentsModule.FundViewItem
import com.quantum.wallet.bankwallet.modules.coin.investments.CoinInvestmentsModule.ViewItem
import com.quantum.wallet.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.CoinInvestment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class CoinInvestmentsViewModel(
    private val service: CoinInvestmentsService,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val viewItemsLiveData = MutableLiveData<List<ViewItem>>()

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

    private fun handleServiceState(state: DataState<List<CoinInvestment>>) {
        state.dataOrNull?.let {
            viewStateLiveData.postValue(ViewState.Success)
            sync(it)
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

    private fun sync(investments: List<CoinInvestment>) {
        viewItemsLiveData.postValue(investments.map { viewItem(it) })
    }

    private fun viewItem(investment: CoinInvestment): ViewItem {
        val amount = investment.amount?.let {
            numberFormatter.formatFiatShort(it, service.usdCurrency.symbol, 2)
        } ?: "---"
        val dateString = DateHelper.formatDate(investment.date, "MMM dd, yyyy")

        return ViewItem(
            amount = amount,
            info = "${investment.round} - $dateString",
            fundViewItems = investment.funds.map {
                FundViewItem(it.name, it.logoUrl, it.isLead, it.website)
            }
        )
    }
}
