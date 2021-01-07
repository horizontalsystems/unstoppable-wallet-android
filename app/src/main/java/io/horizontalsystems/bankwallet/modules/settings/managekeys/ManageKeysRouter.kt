package io.horizontalsystems.bankwallet.modules.settings.managekeys

import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class ManageKeysRouter : ManageKeysModule.IRouter {

    val showRestore = SingleLiveEvent<PredefinedAccountType>()
    val showCreateWalletLiveEvent = SingleLiveEvent<PredefinedAccountType>()
    val showBackupModule = SingleLiveEvent<Pair<Account, PredefinedAccountType>>()
    val closeEvent = SingleLiveEvent<Void>()
    val showAddressFormat = SingleLiveEvent<Unit>()


    override fun showRestore(predefinedAccountType: PredefinedAccountType) {
        showRestore.postValue(predefinedAccountType)
    }

    override fun showCreateWallet(predefinedAccountType: PredefinedAccountType) {
        showCreateWalletLiveEvent.postValue(predefinedAccountType)
    }

    override fun showBackup(account: Account, predefinedAccountType: PredefinedAccountType) {
        showBackupModule.postValue(Pair(account, predefinedAccountType))
    }

    override fun showAddressFormat() {
        showAddressFormat.call()
    }

    override fun close() {
        closeEvent.call()
    }
}
