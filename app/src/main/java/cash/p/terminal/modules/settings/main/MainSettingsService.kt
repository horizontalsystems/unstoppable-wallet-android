package cash.p.terminal.modules.settings.main

import cash.p.terminal.R
import cash.p.terminal.core.IAccountManager
import cash.p.terminal.core.IBackupManager
import cash.p.terminal.core.ITermsManager
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.modules.walletconnect.WCManager
import cash.p.terminal.modules.walletconnect.WCSessionManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class MainSettingsService(
    private val backupManager: IBackupManager,
    private val systemInfoManager: ISystemInfoManager,
    private val termsManager: ITermsManager,
    private val pinComponent: IPinComponent,
    private val wcSessionManager: WCSessionManager,
    private val wcManager: WCManager,
    private val accountManager: IAccountManager,
    private val appConfigProvider: AppConfigProvider
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    val appWebPageLink = appConfigProvider.appWebPageLink
    private val backedUpSubject = BehaviorSubject.create<Boolean>()
    val backedUpObservable: Observable<Boolean> get() = backedUpSubject

    private val pinSetSubject = BehaviorSubject.create<Boolean>()
    val pinSetObservable: Observable<Boolean> get() = pinSetSubject

    val termsAccepted by termsManager::allTermsAccepted
    val termsAcceptedFlow by termsManager::termsAcceptedSignalFlow

    private val walletConnectSessionCountSubject = BehaviorSubject.create<Int>()
    val walletConnectSessionCountObservable: Observable<Int> get() = walletConnectSessionCountSubject

    val hasNonStandardAccount: Boolean
        get() = accountManager.hasNonStandardAccount

    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (Translator.getString(R.string.is_release) == "false") {
                appVersion += " (${appConfigProvider.appBuild})"
            }

            return appVersion
        }

    val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    val pendingRequestCountFlow by wcSessionManager::pendingRequestCountFlow

    val walletConnectSessionCount: Int
        get() = wcSessionManager.sessions.count()

    val isPinSet: Boolean
        get() = pinComponent.isPinSet

    fun start() {
        coroutineScope.launch {
            backupManager.allBackedUpFlowable.asFlow().collect {
                backedUpSubject.onNext(it)
            }
        }
        coroutineScope.launch {
            wcSessionManager.sessionsFlow.collect{
                walletConnectSessionCountSubject.onNext(walletConnectSessionCount)
            }
        }
        coroutineScope.launch {
            pinComponent.pinSetFlowable.asFlow().collect {
                pinSetSubject.onNext(pinComponent.isPinSet)
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun getWalletConnectSupportState(): WCManager.SupportState {
        return wcManager.getWalletConnectSupportState()
    }
}
