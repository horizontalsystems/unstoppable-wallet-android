package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class ManageKeysRouter : ManageKeysModule.IRouter {

    val showRestoreKeyInput = SingleLiveEvent<PredefinedAccountType>()
    val showCoinSettingsEvent = SingleLiveEvent<Unit>()
    val showCoinManager = SingleLiveEvent<Pair<PredefinedAccountType, AccountType>>()
    val showCreateWalletLiveEvent = SingleLiveEvent<PredefinedAccountType>()
    val showBackupModule = SingleLiveEvent<Pair<Account, PredefinedAccountType>>()
    val closeEvent = SingleLiveEvent<Void>()


    override fun showRestoreKeyInput(predefinedAccountType: PredefinedAccountType) {
        showRestoreKeyInput.postValue(predefinedAccountType)
    }

    override fun showCoinSettings() {
        showCoinSettingsEvent.call()
    }

    override fun showCoinManager(predefinedAccountType: PredefinedAccountType, accountType: AccountType) {
        showCoinManager.postValue(Pair(predefinedAccountType, accountType))
    }

    override fun showCreateWallet(predefinedAccountType: PredefinedAccountType) {
        showCreateWalletLiveEvent.postValue(predefinedAccountType)
    }

    override fun showBackup(account: Account, predefinedAccountType: PredefinedAccountType) {
        showBackupModule.postValue(Pair(account, predefinedAccountType))
    }

    override fun close() {
        closeEvent.call()
    }
}
