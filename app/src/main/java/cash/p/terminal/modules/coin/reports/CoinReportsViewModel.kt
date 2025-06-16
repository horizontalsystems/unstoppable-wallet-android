package cash.p.terminal.modules.coin.reports

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.ui_compose.entities.ViewState
import cash.p.terminal.modules.coin.reports.CoinReportsModule.ReportViewItem
import io.horizontalsystems.core.helpers.DateHelper
import cash.p.terminal.wallet.models.CoinReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class CoinReportsViewModel(private val service: CoinReportsService) : ViewModel() {
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)
    val isRefreshingLiveData = MutableLiveData<Boolean>()
    val reportViewItemsLiveData = MutableLiveData<List<ReportViewItem>>()

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

    private fun handleServiceState(state: DataState<List<CoinReport>>) {
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
