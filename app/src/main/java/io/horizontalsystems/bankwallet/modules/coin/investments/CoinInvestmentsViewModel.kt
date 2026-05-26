package io.horizontalsystems.bankwallet.modules.coin.investments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.logoUrl
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsModule.FundViewItem
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsModule.ViewItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.CoinInvestment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel(assistedFactory = CoinInvestmentsViewModel.Factory::class)
class CoinInvestmentsViewModel @AssistedInject constructor(
    @Assisted coinUid: String,
    marketKit: MarketKitWrapper,
    currencyManager: CurrencyManager,
    private val numberFormatter: IAppNumberFormatter,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(coinUid: String): CoinInvestmentsViewModel
    }

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val viewItemsLiveData = MutableLiveData<List<ViewItem>>()

    private val service = CoinInvestmentsService(coinUid, marketKit, currencyManager)

    init {
        viewModelScope.launch {
            service.stateObservable.asFlow()
                .catch { viewStateLiveData.postValue(ViewState.Error(it)) }
                .collect { state -> handleServiceState(state) }
        }
        service.start()
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

    private fun handleServiceState(state: DataState<List<CoinInvestment>>) {
        state.dataOrNull?.let {
            viewStateLiveData.postValue(ViewState.Success)
            sync(it)
        }
        state.errorOrNull?.let {
            viewStateLiveData.postValue(ViewState.Error(it))
        }
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
