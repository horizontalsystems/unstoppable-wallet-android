package io.horizontalsystems.bankwallet.modules.settings.about

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.core.ISystemInfoManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class AboutService(
    appConfigProvider: AppConfigProvider,
    private val termsManager: ITermsManager,
    private val systemInfoManager: ISystemInfoManager
) {

    private val termsAcceptedSubject = BehaviorSubject.create<Boolean>()
    val termsAcceptedObservable: Observable<Boolean> get() = termsAcceptedSubject

    private var disposables: CompositeDisposable = CompositeDisposable()

    val githubLink: String = appConfigProvider.appGithubLink
    val appWebPageLink: String = appConfigProvider.appWebPageLink
    val reportEmail: String = appConfigProvider.reportEmail

    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (Translator.getString(R.string.is_release) == "false") {
                appVersion += " (${BuildConfig.VERSION_CODE})"
            }

            return appVersion
        }

    val termsAccepted: Boolean
        get() = termsManager.termsAccepted

    fun start() {
        disposables.add(termsManager.termsAcceptedSignal.subscribe {
            termsAcceptedSubject.onNext(it)
        })
    }

    fun stop() {
        disposables.clear()
    }

}
