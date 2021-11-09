package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.bankwallet.entities.InitialSyncSetting
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortingType
import io.horizontalsystems.core.SingleLiveEvent

class PrivacySettingsViewModel : ViewModel(), PrivacySettingsModule.IPrivacySettingsView, PrivacySettingsModule.IPrivacySettingsRouter {
    lateinit var delegate: PrivacySettingsModule.IPrivacySettingsViewDelegate

    val showPrivacySettingsInfo = SingleLiveEvent<Unit>()
    val torEnabledLiveData = MutableLiveData<Boolean>()
    val transactionOrderingLiveData = MutableLiveData<TransactionDataSortingType>()
    val showAppRestartAlertForTor = SingleLiveEvent<Boolean>()
    val showNotificationsNotEnabledAlert = SingleLiveEvent<Unit>()
    val restoreWalletSettingsViewItems = SingleLiveEvent<List<PrivacySettingsViewItem>>()
    val showSyncModeSelectorDialog = SingleLiveEvent<Pair<List<SyncMode>, InitialSyncSetting>>()
    val showTransactionsSortingSelectorDialog = SingleLiveEvent<Pair<List<TransactionDataSortingType>, TransactionDataSortingType>>()
    val setTorConnectionStatus = SingleLiveEvent<TorStatus>()

    val restartApp = SingleLiveEvent<Unit>()

    fun init() {
        PrivacySettingsModule.init(this, this)
        delegate.viewDidLoad()
    }

    // IView
    override fun showPrivacySettingsInfo() {
        showPrivacySettingsInfo.postValue(Unit)
    }

    override fun showNotificationsNotEnabledAlert() {
        showNotificationsNotEnabledAlert.call()
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

    override fun setRestoreWalletSettingsViewItems(items: List<PrivacySettingsViewItem>) {
        restoreWalletSettingsViewItems.postValue(items)
    }

    override fun showSyncModeSelectorDialog(syncModeOptions: List<SyncMode>, initialSyncSetting: InitialSyncSetting) {
        showSyncModeSelectorDialog.postValue(Pair(syncModeOptions, initialSyncSetting))
    }

    // IRouter

    override fun restartApp() {
        restartApp.call()
    }

    override fun onCleared() {
        delegate.clear()
    }
}
