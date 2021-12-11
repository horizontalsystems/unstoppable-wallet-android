package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.Disposable

class AboutViewModel(
    private val appConfigProvider: AppConfigProvider,
    private val clipboardManager: IClipboardManager,
    termsManager: ITermsManager,
    systemInfoManager: ISystemInfoManager
) : ViewModel() {

    val openLinkLiveData = SingleLiveEvent<String>()
    val showShareAppLiveData = SingleLiveEvent<String>()
    val termsAcceptedData = MutableLiveData<Boolean>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    val appVersion = systemInfoManager.appVersion
    val reportEmail = appConfigProvider.reportEmail

    var disposable: Disposable? = null

    init {
        termsAcceptedData.postValue(termsManager.termsAccepted)

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

    fun didFailSendMail() {
        clipboardManager.copyText(reportEmail)
        showCopiedLiveEvent.call()
    }

}
