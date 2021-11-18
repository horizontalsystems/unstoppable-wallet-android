package io.horizontalsystems.bankwallet.modules.coin.investments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsModule.FundViewItem
import io.horizontalsystems.bankwallet.modules.coin.investments.CoinInvestmentsModule.ViewItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.CoinInvestment
import io.reactivex.disposables.CompositeDisposable

class CoinInvestmentsViewModel(
    private val service: CoinInvestmentsService,
    private val numberFormatter: IAppNumberFormatter
) : ViewModel() {
    private val disposables = CompositeDisposable()

    val viewStateLiveData = MutableLiveData<ViewState>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val viewItemsLiveData = MutableLiveData<List<ViewItem>>()

    init {
        service.stateObservable
            .subscribeIO({ state ->
                loadingLiveData.postValue(state == DataState.Loading)

                when (state) {
                    is DataState.Success -> {
                        viewStateLiveData.postValue(ViewState.Success)

                        sync(state.data)
                    }
                    is DataState.Error -> {
                        viewStateLiveData.postValue(ViewState.Error)
                    }
                }
            }, {
                viewStateLiveData.postValue(ViewState.Error)
            }).let {
                disposables.add(it)
            }

        service.start()
    }

    fun refresh() {
        service.refresh()
    }

    fun onErrorClick() {
        service.refresh()
    }

    private fun sync(investments: List<CoinInvestment>) {
        viewItemsLiveData.postValue(investments.map { viewItem(it) })
    }

    private fun viewItem(investment: CoinInvestment): ViewItem {
        return ViewItem(
            amount = numberFormatter.formatCurrencyValueAsShortened(CurrencyValue(service.currency, investment.amount)),
            info = "${investment.round} - ${DateHelper.formatDate(investment.date, "MMM dd, yyyy")}",
            fundViewItems = investment.funds.map { fund ->
                FundViewItem(
                    name = fund.name,
                    logoUrl = "https://markets.nyc3.digitaloceanspaces.com/fund-icons/ios/${fund.uid}@3x.png",
                    isLead = fund.isLead,
                    url = fund.website
                )
            }
        )
    }
}
