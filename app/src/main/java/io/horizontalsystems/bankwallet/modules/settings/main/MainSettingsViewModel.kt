package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.CounterType
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class MainSettingsViewModel(
    private val service: MainSettingsService,
    val companyWebPage: String
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    val manageWalletShowAlertLiveData = MutableLiveData(shouldShowAlertForManageWallet(service.allBackedUp, service.hasNonStandardAccount))
    val securityCenterShowAlertLiveData = MutableLiveData(!service.isPinSet)
    val aboutAppShowAlertLiveData = MutableLiveData(!service.termsAccepted)
    val wcCounterLiveData = MutableLiveData<CounterType?>(null)
    val baseCurrencyLiveData = MutableLiveData(service.baseCurrency)
    val languageLiveData = MutableLiveData(service.currentLanguageDisplayName)
    val appVersion by service::appVersion
    val appWebPageLink by service::appWebPageLink

    private var wcSessionsCount = service.walletConnectSessionCount
    private var wcPendingRequestCount = 0

    init {
        viewModelScope.launch {
            service.termsAcceptedFlow.collect {
                aboutAppShowAlertLiveData.postValue(!it)
            }
        }

        service.backedUpObservable
            .subscribeIO { manageWalletShowAlertLiveData.postValue(shouldShowAlertForManageWallet(it, service.hasNonStandardAccount)) }
            .let { disposables.add(it) }

        service.pinSetObservable
            .subscribeIO { securityCenterShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.baseCurrencyObservable
            .subscribeIO { baseCurrencyLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.walletConnectSessionCountObservable
            .subscribeIO {
                wcSessionsCount = it
                syncCounter()
            }
            .let { disposables.add(it) }

        viewModelScope.launch {
            service.pendingRequestCountFlow.collect {
                wcPendingRequestCount = it
                syncCounter()
            }
        }
        syncCounter()
        service.start()
    }

    private fun shouldShowAlertForManageWallet(allBackedUp: Boolean, hasNonStandardAccount: Boolean): Boolean {
        return !allBackedUp || hasNonStandardAccount
    }
    // ViewModel

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun getWalletConnectSupportState(): WCManager.SupportState {
        return service.getWalletConnectSupportState()
    }

    private fun syncCounter() {
        if (wcPendingRequestCount > 0) {
            wcCounterLiveData.postValue(CounterType.PendingRequestCounter(wcPendingRequestCount))
        } else if (wcSessionsCount > 0) {
            wcCounterLiveData.postValue(CounterType.SessionCounter(wcSessionsCount))
        } else {
            wcCounterLiveData.postValue(null)
        }
    }
}
