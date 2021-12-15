package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionManager
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.ILanguageManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class MainSettingsService(
    private val localStorage: ILocalStorage,
    private val backupManager: IBackupManager,
    private val languageManager: ILanguageManager,
    private val systemInfoManager: ISystemInfoManager,
    private val currencyManager: ICurrencyManager,
    private val termsManager: ITermsManager,
    private val pinComponent: IPinComponent,
    private val walletConnectSessionManager: WalletConnectSessionManager
) {

    private val stateUpdatedSubject = BehaviorSubject.create<Unit>()
    val stateUpdatedObservable: Observable<Unit> get() = stateUpdatedSubject

    private var disposables: CompositeDisposable = CompositeDisposable()

    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (Translator.getString(R.string.is_release) == "false") {
                appVersion += " (${BuildConfig.VERSION_CODE})"
            }

            return appVersion
        }

    val themeName: Int
        get() = localStorage.currentTheme.getTitle()

    val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    val walletConnectSessionCount: Int
        get() = walletConnectSessionManager.sessions.count()

    val currentLanguageDisplayName: String
        get() = languageManager.currentLanguageName

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    val termsAccepted: Boolean
        get() = termsManager.termsAccepted

    val isPinSet: Boolean
        get() = pinComponent.isPinSet

    val launchScreen: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    fun start() {
        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            stateUpdatedSubject.onNext(Unit)
        })

        disposables.add(walletConnectSessionManager.sessionsObservable.subscribe {
            stateUpdatedSubject.onNext(Unit)
        })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal.subscribe {
            stateUpdatedSubject.onNext(Unit)
        })

        disposables.add(termsManager.termsAcceptedSignal.subscribe {
            stateUpdatedSubject.onNext(Unit)
        })

        disposables.add(pinComponent.pinSetFlowable.subscribe {
            stateUpdatedSubject.onNext(Unit)
        })
    }

    fun stop() {
        disposables.clear()
    }

    fun setAppRelaunchingFromSettings() {
        localStorage.relaunchBySettingChange = true
    }
}
