package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.disposables.Disposable

class AboutViewModel(
    private val service: AboutService,
) : ViewModel() {

    val githubLink by service::githubLink
    val appWebPageLink by service::appWebPageLink
    val reportEmail by service::reportEmail
    val appVersion by service::appVersion

    val termsShowAlertLiveData = MutableLiveData(!service.termsAccepted)

    var disposable: Disposable? = null

    init {
        service.termsAcceptedObservable
            .subscribeIO { termsShowAlertLiveData.postValue(!it) }
            .let { disposable = it }

        service.start()
    }

    override fun onCleared() {
        service.stop()
        disposable?.dispose()
    }

}
