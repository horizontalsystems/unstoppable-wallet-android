package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.disposables.CompositeDisposable

class MainSettingsViewModel(
    private val service: MainSettingsService,
    val companyWebPage: String,
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    val manageWalletShowAlertLiveData = MutableLiveData(!service.allBackedUp)
    val securityCenterShowAlertLiveData = MutableLiveData(!service.isPinSet)
    val aboutAppShowAlertLiveData = MutableLiveData(!service.termsAccepted)
    val walletConnectSessionCountLiveData = MutableLiveData(service.walletConnectSessionCount)
    val launchScreenLiveData = MutableLiveData(service.launchScreen)
    val baseCurrencyLiveData = MutableLiveData(service.baseCurrency)
    val languageLiveData = MutableLiveData(service.currentLanguageDisplayName)
    val themeLiveData = MutableLiveData(service.themeName)

    val appVersion by service::appVersion

    init {

        service.backedUpObservable
            .subscribeIO { securityCenterShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.pinSetObservable
            .subscribeIO { securityCenterShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.baseCurrencyObservable
            .subscribeIO { baseCurrencyLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.termsAcceptedObservable
            .subscribeIO { aboutAppShowAlertLiveData.postValue(!it) }
            .let { disposables.add(it) }

        service.walletConnectSessionCountObservable
            .subscribeIO { walletConnectSessionCountLiveData.postValue(it) }
            .let { disposables.add(it) }

        service.start()
    }

    fun onLanguageChange() {
        service.setAppRelaunchingFromSettings()
    }

    // ViewModel

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

}
