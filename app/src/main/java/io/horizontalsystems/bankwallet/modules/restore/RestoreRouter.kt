package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestoreRouter : RestoreModule.IRouter {

    val showRestoreCoins = SingleLiveEvent<Pair<PredefinedAccountType, AccountType>>()
    val showKeyInputEvent = SingleLiveEvent<PredefinedAccountType>()
    val closeEvent = SingleLiveEvent<Unit>()
    val showCoinSettingsEvent = SingleLiveEvent<Unit>()

    override fun showKeyInput(predefinedAccountType: PredefinedAccountType) {
        showKeyInputEvent.postValue(predefinedAccountType)
    }

    override fun showRestoreCoins(predefinedAccountType: PredefinedAccountType, accountType: AccountType) {
        showRestoreCoins.postValue(Pair(predefinedAccountType, accountType))
    }

    override fun showCoinSettings() {
        showCoinSettingsEvent.call()
    }

    override fun close() {
        closeEvent.call()
    }
}
