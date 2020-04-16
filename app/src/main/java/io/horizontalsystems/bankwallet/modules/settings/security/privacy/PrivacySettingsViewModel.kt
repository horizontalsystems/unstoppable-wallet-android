package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortingType
import io.horizontalsystems.core.SingleLiveEvent

class PrivacySettingsViewModel : ViewModel(), PrivacySettingsModule.IPrivacySettingsView, PrivacySettingsModule.IPrivacySettingsRouter {
    lateinit var delegate: PrivacySettingsModule.IPrivacySettingsViewDelegate

    val torEnabledLiveData = MutableLiveData<Boolean>()
    val transactionOrderingLiveData = MutableLiveData<TransactionDataSortingType>()
    val showAppRestartAlertForTor = SingleLiveEvent<Boolean>()
    val showNotificationsNotEnabledAlert = SingleLiveEvent<Unit>()
    val showTorPrerequisitesAlert = SingleLiveEvent<Unit>()
    val communicationSettingsViewItems = SingleLiveEvent<List<PrivacySettingsViewItem>>()
    val restoreWalletSettingsViewItems = SingleLiveEvent<List<PrivacySettingsViewItem>>()
    val showSyncModeSelectorDialog = SingleLiveEvent<Pair<List<SyncMode>, SyncMode>>()
    val showTransactionsSortingSelectorDialog = SingleLiveEvent<Pair<List<TransactionDataSortingType>, TransactionDataSortingType>>()
    val showCommunicationSelectorDialog = SingleLiveEvent<Pair<List<CommunicationMode>, CommunicationMode>>()
    val showRestoreModeChangeAlert = SingleLiveEvent<Pair<Coin, SyncMode>>()
    val showCommunicationModeChangeAlert = SingleLiveEvent<Pair<Coin, CommunicationMode>>()
    val setTorConnectionStatus = SingleLiveEvent<TorStatus>()

    val restartApp = SingleLiveEvent<Unit>()

    fun init() {
        PrivacySettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    // IView

    override fun showNotificationsNotEnabledAlert() {
        showNotificationsNotEnabledAlert.call()
    }

    override fun showTorPrerequisitesAlert() {
        showTorPrerequisitesAlert.call()
    }

    override fun toggleTorEnabled(torEnabled: Boolean) {
        torEnabledLiveData.postValue(torEnabled)
    }

    override fun showTransactionsSortingOptions(items: List<TransactionDataSortingType>, selectedItem: TransactionDataSortingType) {
        showTransactionsSortingSelectorDialog.postValue(Pair(items, selectedItem))
    }

    override fun setTransactionsOrdering(transactionsOrdering: TransactionDataSortingType) {
        transactionOrderingLiveData.postValue(transactionsOrdering)
    }

    override fun setTorConnectionStatus(connectionStatus: TorStatus) {
        setTorConnectionStatus.postValue(connectionStatus)
    }

    override fun showRestartAlert(checked: Boolean) {
        showAppRestartAlertForTor.postValue(checked)
    }

    override fun setCommunicationSettingsViewItems(items: List<PrivacySettingsViewItem>) {
        communicationSettingsViewItems.postValue(items)
    }

    override fun setRestoreWalletSettingsViewItems(items: List<PrivacySettingsViewItem>) {
        restoreWalletSettingsViewItems.postValue(items)
    }

    override fun showCommunicationSelectorDialog(communicationModeOptions: List<CommunicationMode>, selected: CommunicationMode) {
        showCommunicationSelectorDialog.postValue(Pair(communicationModeOptions, selected))
    }

    override fun showSyncModeSelectorDialog(syncModeOptions: List<SyncMode>, selected: SyncMode) {
        showSyncModeSelectorDialog.postValue(Pair(syncModeOptions, selected))
    }

    override fun showRestoreModeChangeAlert(coin: Coin, selectedSyncMode: SyncMode) {
        showRestoreModeChangeAlert.postValue(Pair(coin, selectedSyncMode))
    }

    override fun showCommunicationModeChangeAlert(coin: Coin, selectedCommunication: CommunicationMode) {
        showCommunicationModeChangeAlert.postValue(Pair(coin, selectedCommunication))
    }

    // IRouter

    override fun restartApp() {
        restartApp.call()
    }

}
