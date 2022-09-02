package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class MainSettingsViewModel(
    private val service: MainSettingsService,
    val companyWebPage: String,
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    val manageWalletShowAlertLiveData = MutableLiveData(!service.allBackedUp)
    val securityCenterShowAlertLiveData = MutableLiveData(!service.isPinSet)
    val aboutAppShowAlertLiveData = MutableLiveData(!service.termsAccepted)
    val walletConnectSessionCountLiveData = MutableLiveData(service.walletConnectSessionCount)
    val baseCurrencyLiveData = MutableLiveData(service.baseCurrency)
    val languageLiveData = MutableLiveData(service.currentLanguageDisplayName)
    val appVersion by service::appVersion

    init {
        viewModelScope.launch {
            service.termsAcceptedFlow.collect {
                aboutAppShowAlertLiveData.postValue(!it)
            }
        }

        service.backedUpObservable
            .subscribeIO { manageWalletShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.pinSetObservable
            .subscribeIO { securityCenterShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.baseCurrencyObservable
            .subscribeIO { baseCurrencyLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.walletConnectSessionCountObservable
            .subscribeIO { walletConnectSessionCountLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.start()
    }

    // ViewModel

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun getWalletConnectSupportState() : WC1Manager.SupportState {
        return service.getWalletConnectSupportState()
    }
}
