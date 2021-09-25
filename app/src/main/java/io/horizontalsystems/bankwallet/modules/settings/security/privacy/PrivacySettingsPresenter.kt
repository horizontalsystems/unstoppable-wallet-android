package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsType.RestoreModeSettingType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.views.ListPosition

class PrivacySettingsPresenter(
    private val interactor: PrivacySettingsModule.IPrivacySettingsInteractor,
    private val router: PrivacySettingsModule.IPrivacySettingsRouter
) : PrivacySettingsModule.IPrivacySettingsViewDelegate, PrivacySettingsModule.IPrivacySettingsInteractorDelegate {

    var view: PrivacySettingsModule.IPrivacySettingsView? = null

    private var openedPrivacySettings: PrivacySettingsViewItem? = null
    private val needToRestartAppForTor: Boolean
        get() = interactor.wallets.isNotEmpty()

    private val isActiveAccountCreated: Boolean
        get() = interactor.activeAccount?.origin == AccountOrigin.Created

    private val syncItems: List<PrivacySettingsViewItem> =
        interactor.syncSettings().mapIndexed { index, (initialSyncSetting, coin, changeable) ->
            PrivacySettingsViewItem(
                coin.name,
                coin,
                RestoreModeSettingType(initialSyncSetting.syncMode),
                changeable,
                listPosition = ListPosition.getListPosition(interactor.syncSettings().size, index)
            )
        }

    private val syncModeOptions = listOf(SyncMode.Fast, SyncMode.Slow)

    override fun viewDidLoad() {
        interactor.subscribeToTorStatus()

        view?.toggleTorEnabled(interactor.isTorEnabled)
        view?.setTransactionsOrdering(interactor.transactionsSortingType)

        if (!isActiveAccountCreated)
            view?.setRestoreWalletSettingsViewItems(syncItems)
    }

    override fun didSwitchTorEnabled(checked: Boolean) {
        view?.toggleTorEnabled(checked)
        if (checked) {
            if (!interactor.isTorNotificationEnabled) {
                view?.showNotificationsNotEnabledAlert()
                return
            }
        }

        updateTorState(checked)
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
        if (settingType is RestoreModeSettingType) {
            val item = syncItems[position]
            openedPrivacySettings = item
            view?.showSyncModeSelectorDialog(syncModeOptions, settingType.selected, item.coin)
        }
    }

    override fun onTransactionOrderSettingTap() {
        val types = TransactionDataSortingType.values().toList()
        val selectedItem = interactor.transactionsSortingType
        view?.showTransactionsSortingOptions(types, selectedItem)
    }

    override fun onSelectSetting(position: Int) {
        openedPrivacySettings?.let {
            if (it.settingType is RestoreModeSettingType) {
                val syncMode = syncModeOptions[position]
                updateSyncMode(it.coin, syncMode)
            }
        }
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

    private fun updateSyncMode(coin: PlatformCoin, syncMode: SyncMode) {
        (openedPrivacySettings?.settingType as? RestoreModeSettingType)?.selected = syncMode

        interactor.saveSyncModeSetting(InitialSyncSetting(coin.coinType, syncMode))
        view?.setRestoreWalletSettingsViewItems(syncItems)

        openedPrivacySettings = null
    }

}
