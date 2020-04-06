package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsType.Communication
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsType.WalletRestore

class PrivacySettingsPresenter(
        private val interactor: PrivacySettingsModule.IPrivacySettingsInteractor,
        private val router: PrivacySettingsModule.IPrivacySettingsRouter
) : PrivacySettingsModule.IPrivacySettingsViewDelegate, PrivacySettingsModule.IPrivacySettingsInteractorDelegate {

    var view: PrivacySettingsModule.IPrivacySettingsView? = null

    private var openedPrivacySettings: PrivacySettingsViewItem? = null
    private val needToRestartAppForTor: Boolean
        get() = interactor.walletsCount > 0

    private val communicationSettingsViewItems: List<PrivacySettingsViewItem> = listOf(
            interactor.ether(),
            interactor.eos(),
            interactor.binance())
            .mapNotNull { coin ->
                getCommunicationSettingsViewItem(coin)
            }

    private fun getCommunicationSettingsViewItem(coin: Coin): PrivacySettingsViewItem? {
        return when (coin.type) {
            is CoinType.Ethereum -> {
                interactor.communicationSetting(coin.type)?.communicationMode?.let { communicationMode ->
                    PrivacySettingsViewItem(coin, Communication(communicationMode), enabled = true)
                }
            }
            is CoinType.Eos -> PrivacySettingsViewItem(coin, Communication(CommunicationMode.Greymass), enabled = false)
            is CoinType.Binance -> PrivacySettingsViewItem(coin, Communication(CommunicationMode.BinanceDex), enabled = false)
            else -> null
        }
    }

    private val walletRestoreSettingsViewItems: List<PrivacySettingsViewItem> = listOf(
            interactor.bitcoin(),
            interactor.litecoin(),
            interactor.bitcoinCash(),
            interactor.dash())
            .mapNotNull { coin ->
                interactor.syncModeSetting(coin.type)?.syncMode?.let { selected ->
                    PrivacySettingsViewItem(coin, WalletRestore(selected))
                }
            }

    private val communicationModeOptions = listOf(CommunicationMode.Infura, CommunicationMode.Incubed)
    private val syncModeOptions = listOf(SyncMode.Fast, SyncMode.Slow)

    override fun viewDidLoad() {
        interactor.subscribeToTorStatus()

        view?.toggleTorEnabled(interactor.isTorEnabled)
        view?.setTransactionsOrdering(interactor.transactionsSortingType)

        view?.setCommunicationSettingsViewItems(communicationSettingsViewItems)
        view?.setRestoreWalletSettingsViewItems(walletRestoreSettingsViewItems)
    }

    override fun didSwitchTorEnabled(checked: Boolean) {
        if (checked) {
            if (!interactor.isTorNotificationEnabled) {
                view?.showNotificationsNotEnabledAlert()
                return
            }
        }
        if (needToRestartAppForTor) {
            view?.showRestartAlert(checked)
        } else {
            interactor.isTorEnabled = checked
            if (checked) {
                interactor.enableTor()
            } else {
                interactor.disableTor()
            }
        }
    }

    override fun onTorConnectionStatusUpdated(connectionStatus: TorStatus) {
        view?.setTorConnectionStatus(connectionStatus)
        if (connectionStatus == TorStatus.Failed) {
            interactor.isTorEnabled = false
            view?.toggleTorEnabled(false)
        }
    }

    override fun didTapItem(settingType: PrivacySettingsType, position: Int) {
        when (settingType) {
            is Communication -> {
                val item = communicationSettingsViewItems[position]
                if (item.coin == interactor.ether()) {
                    openedPrivacySettings = item
                    view?.showCommunicationSelectorDialog(communicationModeOptions, settingType.selected)
                }
            }
            is WalletRestore -> {
                val item = walletRestoreSettingsViewItems[position]
                openedPrivacySettings = item
                view?.showSyncModeSelectorDialog(syncModeOptions, if (settingType.selected == SyncMode.New) SyncMode.Fast else settingType.selected)
            }
        }
    }

    override fun onTransactionOrderSettingTap() {
        val types = TransactionDataSortingType.values().toList()
        val selectedItem = interactor.transactionsSortingType
        view?.showTransactionsSortingOptions(types, selectedItem)
    }

    override fun onSelectSetting(position: Int) {
        openedPrivacySettings?.let { privacySettings ->

            val coin = privacySettings.coin
            val settingType = privacySettings.settingType

            if (settingType is WalletRestore) {
                val syncMode = syncModeOptions[position]
                onSelectSyncMode(coin, syncMode, settingType.selected)
            } else if (settingType is Communication) {
                val communicationMode = communicationModeOptions[position]
                onSelectCommunicationMode(coin, communicationMode, settingType.selected)
            }
        }
    }

    private fun onSelectCommunicationMode(coin: Coin, selectedValue: CommunicationMode, currentValue: CommunicationMode) {
        if (currentValue != selectedValue && interactor.getWalletForUpdate(coin.type) != null) {
            view?.showCommunicationModeChangeAlert(coin, selectedValue)
        } else {
            updateCommunicationMode(coin, selectedValue)
        }
    }

    private fun onSelectSyncMode(coin: Coin, selectedValue: SyncMode, currentValue: SyncMode) {
        if (currentValue != selectedValue && interactor.getWalletForUpdate(coin.type) != null) {
            view?.showRestoreModeChangeAlert(coin, selectedValue)
        } else {
            updateSyncMode(coin, selectedValue)
        }
    }

    private fun updateSyncMode(coin: Coin, syncMode: SyncMode) {
        (openedPrivacySettings?.settingType as? WalletRestore)?.selected = syncMode

        interactor.saveSyncModeSetting(SyncModeSetting(coin.type, syncMode))
        view?.setRestoreWalletSettingsViewItems(walletRestoreSettingsViewItems)

        openedPrivacySettings = null
    }

    override fun proceedWithSyncModeChange(coin: Coin, syncMode: SyncMode) {
        updateSyncMode(coin, syncMode)

        interactor.getWalletForUpdate(coin.type)?.let {
            interactor.reSyncWallet(it)
        }
    }

    private fun updateCommunicationMode(coin: Coin, communicationMode: CommunicationMode) {
        (openedPrivacySettings?.settingType as? Communication)?.selected = communicationMode

        interactor.saveCommunicationSetting(CommunicationSetting(coin.type, communicationMode))
        view?.setCommunicationSettingsViewItems(communicationSettingsViewItems)

        openedPrivacySettings = null
    }

    override fun proceedWithCommunicationModeChange(coin: Coin, communicationMode: CommunicationMode) {
        updateCommunicationMode(coin, communicationMode)

        interactor.getWalletForUpdate(coin.type)?.let {
            interactor.reSyncWallet(it)
        }
    }

    override fun onSelectTransactionSorting(transactionDataSortingType: TransactionDataSortingType) {
        interactor.transactionsSortingType = transactionDataSortingType
        view?.setTransactionsOrdering(interactor.transactionsSortingType)
    }

    override fun setTorEnabled(checked: Boolean) {
        interactor.isTorEnabled = checked
        if (checked) {
            router.restartApp()
        } else {
            interactor.stopTor()
        }
    }

    override fun didStopTor() {
        if (needToRestartAppForTor) {
            router.restartApp()
        }
    }

}
