package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.app.Activity
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*

object PrivacySettingsModule {

    interface IPrivacySettingsView {

        fun showPrivacySettingsInfo()
        fun showNotificationsNotEnabledAlert()
        fun showTorPrerequisitesAlert()
        fun showRestartAlert(checked: Boolean)
        fun toggleTorEnabled(torEnabled: Boolean)
        fun setBlockchainSettingsVisibility(isVisibile: Boolean)
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
        fun didTapItem(settingType: PrivacySettingsType, position: Int)
        fun onSelectSetting(position: Int)
        fun proceedWithCommunicationModeChange(coin: Coin, communicationMode: CommunicationMode)
        fun onTransactionOrderSettingTap()
        fun onSelectTransactionSorting(transactionDataSortingType: TransactionDataSortingType)
        fun onShowPrivacySettingsInfoClick()
    }

    interface IPrivacySettingsInteractor {
        val walletsCount: Int
        var transactionsSortingType: TransactionDataSortingType
        var isTorEnabled: Boolean
        val isTorNotificationEnabled: Boolean
        fun stopTor()
        fun enableTor()
        fun disableTor()
        fun subscribeToTorStatus()

        fun communicationSetting(coinType: CoinType): CommunicationSetting?
        fun saveCommunicationSetting(communicationSetting: CommunicationSetting)
        fun syncModeSetting(coinType: CoinType): SyncModeSetting?
        fun saveSyncModeSetting(syncModeSetting: SyncModeSetting)
        fun ether(): Coin
        fun eos(): Coin
        fun binance(): Coin
        fun bitcoin(): Coin
        fun litecoin(): Coin
        fun bitcoinCash(): Coin
        fun dash(): Coin
        fun getWalletsForUpdate(coinType: CoinType): List<Wallet>
        fun reSyncWallets(wallets: List<Wallet>)

        fun clear()
        fun isAccountOriginCreated(): Boolean
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
                App.blockchainSettingsManager,
                App.appConfigProvider,
                App.walletManager,
                App.localStorage,
                App.adapterManager,
                App.accountManager)

        val presenter = PrivacySettingsPresenter(interactor, router)
        interactor.delegate = presenter
        view.delegate = presenter
        presenter.view = view
    }

    fun start(activity: Activity) {
        activity.startActivity(Intent(activity, PrivacySettingsActivity::class.java))
    }

}
