package io.horizontalsystems.bankwallet.modules.coin.audits

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsModule.AuditViewItem
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsModule.ViewItem
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.Auditor
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoinAuditsViewModel(
    private val service: CoinAuditsService
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
                        viewStateLiveData.postValue(ViewState.Error(state.error))
                    }
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

    override fun onCleared() {
        disposables.clear()
        service.stop()
    }

    private fun sync(auditors: List<Auditor>) {
        viewItemsLiveData.postValue(auditors.map { viewItem(it) })
    }

    private fun viewItem(auditor: Auditor): ViewItem {
        return ViewItem(
            name = auditor.name,
            logoUrl = "",
            auditViewItems = auditor.reports.map { report ->
                AuditViewItem(
                    date = report.date?.let { DateHelper.formatDate(it, "MMM dd, yyyy") },
                    name = report.name,
                    issues = TranslatableString.ResString(R.string.CoinPage_Audits_Issues, report.issues),
                    reportUrl = report.link
                )
            }
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
}
