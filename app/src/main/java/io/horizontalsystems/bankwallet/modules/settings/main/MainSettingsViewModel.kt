package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsModule.CounterType
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class MainSettingsViewModel(
    private val service: MainSettingsService,
    val companyWebPage: String
) : ViewModel() {

    val manageWalletShowAlertLiveData = MutableLiveData(shouldShowAlertForManageWallet(service.allBackedUp, service.hasNonStandardAccount))
    val securityCenterShowAlertLiveData = MutableLiveData(!service.isPinSet)
    val aboutAppShowAlertLiveData = MutableLiveData(!service.termsAccepted)
    val wcCounterLiveData = MutableLiveData<CounterType?>(null)
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
        viewModelScope.launch {
            service.backedUpObservable.asFlow().collect {
                manageWalletShowAlertLiveData.postValue(shouldShowAlertForManageWallet(it, service.hasNonStandardAccount))
            }
        }
        viewModelScope.launch {
            service.pinSetObservable.asFlow().collect {
                securityCenterShowAlertLiveData.postValue(!it)
            }
        }
        viewModelScope.launch {
            service.walletConnectSessionCountObservable.asFlow().collect {
                wcSessionsCount = it
                syncCounter()
            }
        }
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
