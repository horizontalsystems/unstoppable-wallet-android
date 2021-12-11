package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.views.ListPosition

object PrivacySettingsModule {

    interface IPrivacySettingsView {

        fun showPrivacySettingsInfo()
        fun showNotificationsNotEnabledAlert()
        fun showRestartAlert(checked: Boolean)
        fun toggleTorEnabled(torEnabled: Boolean)
        fun setRestoreWalletSettingsViewItems(items: List<PrivacySettingsViewItem>)
        fun showSyncModeSelectorDialog(syncModeOptions: List<SyncMode>, initialSyncSetting: InitialSyncSetting)
        fun setTransactionsOrdering(transactionsOrdering: TransactionDataSortingType)
        fun showTransactionsSortingOptions(items: List<TransactionDataSortingType>, selectedItem: TransactionDataSortingType)
        fun setTorConnectionStatus(connectionStatus: TorStatus)
    }

    interface IPrivacySettingsViewDelegate {
        fun viewDidLoad()
        fun didSwitchTorEnabled(checked: Boolean)
        fun updateTorState(checked: Boolean)
        fun setTorEnabled(checked: Boolean)
        fun onItemTap(viewItem: PrivacySettingsViewItem)
        fun onSelectSyncMode(syncMode: SyncMode, coinType: CoinType)
        fun onTransactionOrderSettingTap()
        fun onSelectTransactionSorting(transactionDataSortingType: TransactionDataSortingType)
        fun onShowPrivacySettingsInfoClick()
        fun clear()
    }

    interface IPrivacySettingsInteractor {
        val wallets: List<Wallet>
        val activeAccount: Account?
        var transactionsSortingType: TransactionDataSortingType
        var isTorEnabled: Boolean
        val isTorNotificationEnabled: Boolean
        fun stopTor()
        fun enableTor()
        fun disableTor()
        fun subscribeToTorStatus()

        fun syncSettings(): List<Pair<InitialSyncSetting, Boolean>>
        fun saveSyncModeSetting(syncModeSetting: InitialSyncSetting)

        fun clear()
    }

    interface IPrivacySettingsInteractorDelegate {
        fun didStopTor()
        fun onTorConnectionStatusUpdated(connectionStatus: TorStatus)
    }

    interface IPrivacySettingsRouter {
        fun restartApp()
    }

    fun init(view: PrivacySettingsViewModel, router: IPrivacySettingsRouter) {
        val interactor = PrivacySettingsInteractor(
                App.pinComponent,
                App.torKitManager,
                App.initialSyncModeSettingsManager,
                App.walletManager,
                App.accountManager,
                App.localStorage)

        val presenter = PrivacySettingsPresenter(interactor, router)
        interactor.delegate = presenter
        view.delegate = presenter
        presenter.view = view
    }
}

data class PrivacySettingsViewItem(
    val initialSyncSetting: InitialSyncSetting,
    var enabled: Boolean = true,
    val listPosition: ListPosition
)
