package io.horizontalsystems.bankwallet.modules.coin.reports

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.reports.CoinReportsModule.ReportViewItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.CoinReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel(assistedFactory = CoinReportsViewModel.Factory::class)
class CoinReportsViewModel @AssistedInject constructor(
    @Assisted coinUid: String,
    marketKit: MarketKitWrapper,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(coinUid: String): CoinReportsViewModel
    }

    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val reportViewItemsLiveData = MutableLiveData<List<ReportViewItem>>()

    private val service = CoinReportsService(coinUid, marketKit)

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

    private fun handleServiceState(state: DataState<List<CoinReport>>) {
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

    private fun sync(reports: List<CoinReport>) {
        reportViewItemsLiveData.postValue(reports.map { viewItem(it) })
    }

    private fun viewItem(report: CoinReport): ReportViewItem {
        return ReportViewItem(
            author = report.author,
            title = report.title,
            body = report.body,
            date = DateHelper.formatDate(report.date, "MMM dd, yyyy"),
            url = report.url
        )
    }
}
