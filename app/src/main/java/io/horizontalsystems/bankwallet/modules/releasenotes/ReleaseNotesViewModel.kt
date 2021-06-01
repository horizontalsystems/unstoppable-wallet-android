package io.horizontalsystems.bankwallet.modules.releasenotes

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.managers.ReleaseNotesManager
import io.horizontalsystems.core.SingleLiveEvent

class ReleaseNotesViewModel(
        private val appConfigProvider: IAppConfigProvider,
        releaseNotesManager: ReleaseNotesManager
) : ViewModel() {

    val releaseNotesUrl = releaseNotesManager.releaseNotesUrl
    val openLinkLiveData = SingleLiveEvent<String>()

    fun onTwitterTap() {
        openLinkLiveData.postValue(appConfigProvider.appTwitterLink)
    }

    fun onTelegramTap() {
        openLinkLiveData.postValue(appConfigProvider.appTelegramLink)
    }

    fun onRedditTap() {
        openLinkLiveData.postValue(appConfigProvider.appRedditLink)
    }

}
