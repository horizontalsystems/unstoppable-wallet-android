package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.InitialSyncSetting
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortingType
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.views.ListPosition

class PrivacySettingsPresenter(
    private val interactor: PrivacySettingsModule.IPrivacySettingsInteractor,
    private val router: PrivacySettingsModule.IPrivacySettingsRouter
) : PrivacySettingsModule.IPrivacySettingsViewDelegate,
    PrivacySettingsModule.IPrivacySettingsInteractorDelegate {

    var view: PrivacySettingsModule.IPrivacySettingsView? = null

    private val needToRestartAppForTor: Boolean
        get() = interactor.wallets.isNotEmpty()

    private val isActiveAccountCreated: Boolean
        get() = interactor.activeAccount?.origin == AccountOrigin.Created

    private val syncItems: List<PrivacySettingsViewItem>
        get() = interactor.syncSettings().mapIndexed { index, (initialSyncSetting, changeable) ->
            PrivacySettingsViewItem(
                initialSyncSetting,
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

    override fun onItemTap(viewItem: PrivacySettingsViewItem) {
        view?.showSyncModeSelectorDialog(
            syncModeOptions,
            viewItem.initialSyncSetting
        )
    }

    override fun onTransactionOrderSettingTap() {
        val types = TransactionDataSortingType.values().toList()
        val selectedItem = interactor.transactionsSortingType
        view?.showTransactionsSortingOptions(types, selectedItem)
    }

    override fun onSelectSyncMode(syncMode: SyncMode, coinType: CoinType) {
        interactor.saveSyncModeSetting(InitialSyncSetting(coinType, syncMode))
        view?.setRestoreWalletSettingsViewItems(syncItems)
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

}
