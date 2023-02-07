package cash.p.terminal.modules.settings.main

import cash.p.terminal.BuildConfig
import cash.p.terminal.R
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.IBackupManager
import cash.p.terminal.core.ITermsManager
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.Currency
import cash.p.terminal.modules.walletconnect.version1.WC1Manager
import cash.p.terminal.modules.walletconnect.version1.WC1SessionManager
import cash.p.terminal.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class MainSettingsService(
    private val backupManager: IBackupManager,
    private val languageManager: LanguageManager,
    private val systemInfoManager: ISystemInfoManager,
    private val currencyManager: CurrencyManager,
    private val termsManager: ITermsManager,
    private val pinComponent: IPinComponent,
    private val wc1SessionManager: WC1SessionManager,
    private val wc2SessionManager: WC2SessionManager,
    private val wc1Manager: WC1Manager,
    private val accountManager: IAccountManager
) {

    private val backedUpSubject = BehaviorSubject.create<Boolean>()
    val backedUpObservable: Observable<Boolean> get() = backedUpSubject

    private val pinSetSubject = BehaviorSubject.create<Boolean>()
    val pinSetObservable: Observable<Boolean> get() = pinSetSubject

    val termsAccepted by termsManager::allTermsAccepted
    val termsAcceptedFlow by termsManager::termsAcceptedSignalFlow

    private val baseCurrencySubject = BehaviorSubject.create<Currency>()
    val baseCurrencyObservable: Observable<Currency> get() = baseCurrencySubject

    private val walletConnectSessionCountSubject = BehaviorSubject.create<Int>()
    val walletConnectSessionCountObservable: Observable<Int> get() = walletConnectSessionCountSubject

    val hasNonStandardAccount: Boolean
        get() = accountManager.hasNonStandardAccount

    private var disposables: CompositeDisposable = CompositeDisposable()

    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (Translator.getString(R.string.is_release) == "false") {
                appVersion += " (${BuildConfig.VERSION_CODE})"
            }

            return appVersion
        }

    val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    val pendingRequestCountFlow by wc2SessionManager::pendingRequestCountFlow

    val walletConnectSessionCount: Int
        get() = wc1SessionManager.sessions.count() + wc2SessionManager.sessions.count()

    val currentLanguageDisplayName: String
        get() = languageManager.currentLanguageName

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    val isPinSet: Boolean
        get() = pinComponent.isPinSet

    fun start() {
        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            backedUpSubject.onNext(it)
        })

        disposables.add(wc1SessionManager.sessionsObservable.subscribe {
            walletConnectSessionCountSubject.onNext(walletConnectSessionCount)
        })

        disposables.add(wc2SessionManager.sessionsObservable.subscribe {
            walletConnectSessionCountSubject.onNext(walletConnectSessionCount)
        })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal.subscribe {
            baseCurrencySubject.onNext(currencyManager.baseCurrency)
        })

        disposables.add(pinComponent.pinSetFlowable.subscribe {
            pinSetSubject.onNext(pinComponent.isPinSet)
        })
    }

    fun stop() {
        disposables.clear()
    }

    fun getWalletConnectSupportState(): WC1Manager.SupportState {
        return wc1Manager.getWalletConnectSupportState()
    }
}
