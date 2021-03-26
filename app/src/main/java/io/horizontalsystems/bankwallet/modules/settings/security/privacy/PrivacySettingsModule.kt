package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.views.ListPosition

object PrivacySettingsModule {

    interface IPrivacySettingsView {

        fun showPrivacySettingsInfo()
        fun showNotificationsNotEnabledAlert()
        fun showTorPrerequisitesAlert()
        fun showRestartAlert(checked: Boolean)
        fun toggleTorEnabled(torEnabled: Boolean)
        fun setCommunicationSettingsViewItems(items: List<PrivacySettingsViewItem>)
        fun setRestoreWalletSettingsViewItems(items: List<PrivacySettingsViewItem>)
        fun showCommunicationSelectorDialog(communicationModeOptions: List<CommunicationMode>, selected: CommunicationMode, coin: Coin)
        fun showSyncModeSelectorDialog(syncModeOptions: List<SyncMode>, selected: SyncMode, coin: Coin)
        fun showCommunicationModeChangeAlert(coin: Coin, selectedCommunication: CommunicationMode )
        fun setTransactionsOrdering(transactionsOrdering: TransactionDataSortingType)
        fun showTransactionsSortingOptions(items: List<TransactionDataSortingType>, selectedItem: TransactionDataSortingType)
        fun setTorConnectionStatus(connectionStatus: TorStatus)
    }

    interface IPrivacySettingsViewDelegate {
        fun viewDidLoad()
        fun didSwitchTorEnabled(checked: Boolean)
        fun onApplyTorPrerequisites(checked: Boolean)
        fun updateTorState(checked: Boolean)
        fun setTorEnabled(checked: Boolean)
        fun onItemTap(settingType: PrivacySettingsType, position: Int)
        fun onSelectSetting(position: Int)
        fun proceedWithCommunicationModeChange(coin: Coin, communicationMode: CommunicationMode)
        fun onTransactionOrderSettingTap()
        fun onSelectTransactionSorting(transactionDataSortingType: TransactionDataSortingType)
        fun onShowPrivacySettingsInfoClick()
        fun clear()
    }

    interface IPrivacySettingsInteractor {
        val wallets: List<Wallet>
        var transactionsSortingType: TransactionDataSortingType
        var isTorEnabled: Boolean
        val isTorNotificationEnabled: Boolean
        val ethereumCommunicationModes: List<CommunicationMode>
        fun stopTor()
        fun enableTor()
        fun disableTor()
        fun subscribeToTorStatus()

        fun syncSettings(): List<Triple<InitialSyncSetting, Coin, Boolean>>
        fun ethereumConnection(): EthereumRpcMode
        fun saveEthereumRpcModeSetting(rpcModeSetting: EthereumRpcMode)
        fun saveSyncModeSetting(syncModeSetting: InitialSyncSetting)
        val ether: Coin
        val binance: Coin
        val binanceSmartChain: Coin

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
                App.ethereumRpcModeSettingsManager,
                App.coinManager,
                App.walletManager,
                App.localStorage)

        val presenter = PrivacySettingsPresenter(interactor, router)
        interactor.delegate = presenter
        view.delegate = presenter
        presenter.view = view
    }
}

sealed class PrivacySettingsType {
    open val selectedTitle: String = ""

    class CommunicationModeSettingType(var selected: CommunicationMode) : PrivacySettingsType() {
        override val selectedTitle: String
            get() = selected.title
    }

    class RestoreModeSettingType(var selected: SyncMode) : PrivacySettingsType() {
        override val selectedTitle: String
            get() = selected.title
    }
}

data class PrivacySettingsViewItem(
        val title: String,
        val coin: Coin,
        val settingType: PrivacySettingsType,
        var enabled: Boolean = true,
        val listPosition: ListPosition
)
