package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable

class AboutViewModel(
        private val appConfigProvider: IAppConfigProvider,
        termsManager: ITermsManager,
        systemInfoManager: ISystemInfoManager
) : ViewModel() {

    val openLinkLiveData = SingleLiveEvent<String>()
    val showShareAppLiveData = SingleLiveEvent<String>()
    val appVersionLiveData = MutableLiveData<String>()
    val termsAcceptedData = MutableLiveData<Boolean>()

    var disposable: Disposable? = null

    init {
        termsAcceptedData.postValue(termsManager.termsAccepted)
        appVersionLiveData.postValue(systemInfoManager.appVersion)

        disposable = termsManager.termsAcceptedSignal
                .subscribe { allAccepted ->
                    termsAcceptedData.postValue(allAccepted)
                }
    }

    override fun onCleared() {
        disposable?.dispose()
        super.onCleared()
    }

    fun onGithubLinkTap() {
        openLinkLiveData.postValue(appConfigProvider.appGithubLink)
    }

    fun onSiteLinkTap() {
        openLinkLiveData.postValue(appConfigProvider.appWebPageLink)
    }

    fun onTellFriendsTap() {
        showShareAppLiveData.postValue(appConfigProvider.appWebPageLink)
    }

}
