package io.horizontalsystems.bankwallet.modules.settings.main

import com.trustwallet.walletconnect.models.WCPeerMeta
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSessionManager
import io.horizontalsystems.core.*
import io.horizontalsystems.core.entities.Currency
import io.reactivex.disposables.CompositeDisposable

class MainSettingsInteractor(
        private val themeStorage: IThemeStorage,
        private val backupManager: IBackupManager,
        private val languageManager: ILanguageManager,
        private val systemInfoManager: ISystemInfoManager,
        private val currencyManager: ICurrencyManager,
        private val appConfigProvider: IAppConfigProvider,
        private val termsManager: ITermsManager,
        private val pinComponent: IPinComponent,
        private val walletConnectSessionManager: WalletConnectSessionManager
) : MainSettingsModule.IMainSettingsInteractor {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var delegate: MainSettingsModule.IMainSettingsInteractorDelegate? = null

    init {
        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            delegate?.didUpdateAllBackedUp(it)
        })

        disposables.add(walletConnectSessionManager.sessionsObservable.subscribe {
            delegate?.didUpdateWalletConnectSessionCount(it.count())
        })

        disposables.add(currencyManager.baseCurrencyUpdatedSignal.subscribe {
            delegate?.didUpdateBaseCurrency()
        })

        disposables.add(termsManager.termsAcceptedSignal
                .subscribe { allAccepted ->
                    delegate?.didUpdateTermsAccepted(allAccepted)
                })
        disposables.add(pinComponent.pinSetFlowable.subscribe {
            delegate?.didUpdatePinSet()
        })
    }

    override val companyWebPageLink: String
        get() = appConfigProvider.companyWebPageLink

    override val appWebPageLink: String
        get() = appConfigProvider.appWebPageLink

    override val companyTwitterLink: String
        get() = appConfigProvider.companyTwitterLink

    override val companyTelegramLink: String
        get() = appConfigProvider.companyTelegramLink

    override val companyRedditLink: String
        get() = appConfigProvider.companyRedditLink

    override val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    override val walletConnectSessionCount: Int
        get() = walletConnectSessionManager.sessions.count()

    override val currentLanguageDisplayName: String
        get() = languageManager.currentLanguageName

    override val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    override var lightMode: Boolean
        get() = themeStorage.isLightModeOn
        set(value) {
            themeStorage.isLightModeOn = value
        }

    override val termsAccepted: Boolean
        get() = termsManager.termsAccepted

    override val appVersion: String
        get() = systemInfoManager.appVersion

    override val isPinSet: Boolean
        get() = pinComponent.isPinSet

    override fun clear() {
        disposables.clear()
    }

}
