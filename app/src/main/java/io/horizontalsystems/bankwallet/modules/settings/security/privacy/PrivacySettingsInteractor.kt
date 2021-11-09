package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.InitialSyncSetting
import io.horizontalsystems.bankwallet.entities.TransactionDataSortingType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.IPinComponent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class PrivacySettingsInteractor(
        private val pinComponent: IPinComponent,
        private val torManager: ITorManager,
        private val syncModeSettingsManager: IInitialSyncModeSettingsManager,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager,
        private val localStorageManager: ILocalStorage
) : PrivacySettingsModule.IPrivacySettingsInteractor {

    var delegate: PrivacySettingsModule.IPrivacySettingsInteractorDelegate? = null

    private var disposables: CompositeDisposable = CompositeDisposable()

    override var transactionsSortingType: TransactionDataSortingType
        get() = localStorageManager.transactionSortingType
        set(value) {
            localStorageManager.transactionSortingType = value
        }

    override val wallets: List<Wallet>
        get() = walletManager.activeWallets

    override val activeAccount: Account?
        get() = accountManager.activeAccount

    override var isTorEnabled: Boolean
        get() = torManager.isTorEnabled
        set(value) {
            pinComponent.updateLastExitDateBeforeRestart()
            if (value) {
                torManager.enableTor()
            } else {
                torManager.disableTor()
            }
        }

    override val isTorNotificationEnabled: Boolean
        get() = torManager.isTorNotificationEnabled

    override fun subscribeToTorStatus() {
        delegate?.onTorConnectionStatusUpdated(TorStatus.Closed)
        torManager.torObservable
                .subscribe { connectionStatus ->
                    delegate?.onTorConnectionStatusUpdated(connectionStatus)
                }.let {
                    disposables.add(it)
                }
    }

    override fun stopTor() {
        torManager.stop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.didStopTor()
                }, {

                }).let {
                    disposables.add(it)
                }
    }

    override fun enableTor() {
        torManager.start()
    }

    override fun disableTor() {
        torManager.stop()
                .subscribe()
                .let {
                    disposables.add(it)
                }
    }

    override fun syncSettings(): List<Pair<InitialSyncSetting, Boolean>> {
        return syncModeSettingsManager.allSettings()
    }

    override fun saveSyncModeSetting(syncModeSetting: InitialSyncSetting) {
        syncModeSettingsManager.save(syncModeSetting)
    }

    override fun clear() {
        disposables.clear()
    }

}
