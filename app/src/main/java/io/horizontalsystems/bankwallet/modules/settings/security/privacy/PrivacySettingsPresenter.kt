package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsType.CommunicationModeSettingType
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsType.RestoreModeSettingType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.ListPosition

class PrivacySettingsPresenter(
        private val interactor: PrivacySettingsModule.IPrivacySettingsInteractor,
        private val router: PrivacySettingsModule.IPrivacySettingsRouter
) : PrivacySettingsModule.IPrivacySettingsViewDelegate, PrivacySettingsModule.IPrivacySettingsInteractorDelegate {

    var view: PrivacySettingsModule.IPrivacySettingsView? = null

    private var openedPrivacySettings: PrivacySettingsViewItem? = null
    private val needToRestartAppForTor: Boolean
        get() = interactor.wallets.isNotEmpty()

    private val standardCreatedWalletExists: Boolean
        get() = interactor.wallets.firstOrNull {
            it.account.origin == AccountOrigin.Created && it.coin.type.predefinedAccountType == PredefinedAccountType.Standard
        } != null

    private val syncItems: List<PrivacySettingsViewItem> =
            interactor.syncSettings().mapIndexed { index, (initialSyncSetting, coin, changeable) ->
                PrivacySettingsViewItem(
                        coin.title,
                        coin,
                        RestoreModeSettingType(initialSyncSetting.syncMode),
                        changeable,
                        listPosition = ListPosition.getListPosition(interactor.syncSettings().size, index)
                )
            }

    private val communicationSettingsViewItems: List<PrivacySettingsViewItem> = listOf(
            PrivacySettingsViewItem("Ethereum", interactor.ether, CommunicationModeSettingType(CommunicationMode.Infura), enabled = ethereumCommunicationModeCanBeChanged(), listPosition = ListPosition.First),
            PrivacySettingsViewItem("Smart Chain", interactor.binanceSmartChain, CommunicationModeSettingType(CommunicationMode.Nariox), enabled = false, listPosition = ListPosition.Middle),
            PrivacySettingsViewItem("Binance", interactor.binance, CommunicationModeSettingType(CommunicationMode.BinanceDex), enabled = false, listPosition = ListPosition.Last)
    )

    private val communicationModeOptions = listOf(CommunicationMode.Infura)
    private val syncModeOptions = listOf(SyncMode.Fast, SyncMode.Slow)

    override fun viewDidLoad() {
        interactor.subscribeToTorStatus()

        view?.toggleTorEnabled(interactor.isTorEnabled)
        view?.setTransactionsOrdering(interactor.transactionsSortingType)

        view?.setCommunicationSettingsViewItems(communicationSettingsViewItems)

        if (!standardCreatedWalletExists)
            view?.setRestoreWalletSettingsViewItems(syncItems)
    }

    override fun didSwitchTorEnabled(checked: Boolean) {
        view?.toggleTorEnabled(checked)
        if (checked) {
            if (!interactor.isTorNotificationEnabled) {
                view?.showNotificationsNotEnabledAlert()
                return
            }

            // Check if Tor needs to update Blockchain configuration
            if (interactor.ethereumConnection().communicationMode != CommunicationMode.Infura) {

                openedPrivacySettings = communicationSettingsViewItems.find { it.coin.type == CoinType.Ethereum }
                openedPrivacySettings?.enabled = !checked
                onSelectCommunicationModeByTor()
                return
            }
        }

        updateTorState(checked)
    }

    override fun onApplyTorPrerequisites(checked: Boolean) {
        openedPrivacySettings = communicationSettingsViewItems.find { it.coin.type == CoinType.Ethereum }
        openedPrivacySettings?.enabled = !checked && ethereumCommunicationModeCanBeChanged()
        view?.setCommunicationSettingsViewItems(communicationSettingsViewItems)
    }

    override fun updateTorState(checked: Boolean) {
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
        } else if (connectionStatus == TorStatus.Connected) {
            interactor.isTorEnabled = true
            view?.toggleTorEnabled(true)
        }
    }

    override fun onItemTap(settingType: PrivacySettingsType, position: Int) {
        when (settingType) {
            is CommunicationModeSettingType -> {
                val item = communicationSettingsViewItems[position]
                if (item.coin == interactor.ether) {
                    openedPrivacySettings = item
                    view?.showCommunicationSelectorDialog(communicationModeOptions, settingType.selected, item.coin)
                }
            }
            is RestoreModeSettingType -> {
                val item = syncItems[position]
                openedPrivacySettings = item
                view?.showSyncModeSelectorDialog(syncModeOptions, settingType.selected, item.coin)
            }
        }
    }

    override fun onTransactionOrderSettingTap() {
        val types = TransactionDataSortingType.values().toList()
        val selectedItem = interactor.transactionsSortingType
        view?.showTransactionsSortingOptions(types, selectedItem)
    }

    override fun onSelectSetting(position: Int) {
        openedPrivacySettings?.let {
            when (it.settingType) {
                is RestoreModeSettingType -> {
                    val syncMode = syncModeOptions[position]
                    updateSyncMode(it.coin, syncMode)
                }
                is CommunicationModeSettingType -> {
                    val communicationMode = communicationModeOptions[position]
                    updateCommunicationMode(it.coin, communicationMode)
                }
            }
        }
    }

    override fun proceedWithCommunicationModeChange(coin: Coin, communicationMode: CommunicationMode) {
        updateCommunicationMode(coin, communicationMode)
        updateTorState(true)
    }

    override fun onSelectTransactionSorting(transactionDataSortingType: TransactionDataSortingType) {
        interactor.transactionsSortingType = transactionDataSortingType
        view?.setTransactionsOrdering(interactor.transactionsSortingType)
    }

    override fun onShowPrivacySettingsInfoClick() {
        view?.showPrivacySettingsInfo()
    }

    override fun clear() {
        interactor.clear()
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

    private fun ethereumCommunicationModeCanBeChanged() = interactor.ethereumCommunicationModes.size > 1

    private fun onSelectCommunicationModeByTor() {
        val coin = interactor.ether
        val selectedValue = CommunicationMode.Infura

        // include Erc20 wallets for CoinType.Ethereum
        val walletsToUpdate = interactor.wallets.filter { it.coin.type == CoinType.Ethereum || it.coin.type is CoinType.Erc20 }

        if (walletsToUpdate.isNotEmpty()) {
            view?.showCommunicationModeChangeAlert(coin, selectedValue)
        } else {
            view?.showTorPrerequisitesAlert()

            updateCommunicationMode(coin, selectedValue)
        }
    }

    private fun updateSyncMode(coin: Coin, syncMode: SyncMode) {
        (openedPrivacySettings?.settingType as? RestoreModeSettingType)?.selected = syncMode

        interactor.saveSyncModeSetting(InitialSyncSetting(coin.type, syncMode))
        view?.setRestoreWalletSettingsViewItems(syncItems)

        openedPrivacySettings = null
    }

    private fun updateCommunicationMode(coin: Coin, communicationMode: CommunicationMode) {
        (openedPrivacySettings?.settingType as? CommunicationModeSettingType)?.selected = communicationMode

        interactor.saveEthereumRpcModeSetting(EthereumRpcMode(coin.type, communicationMode))
        view?.setCommunicationSettingsViewItems(communicationSettingsViewItems)

        openedPrivacySettings = null
    }

}
