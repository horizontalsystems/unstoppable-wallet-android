package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class ManageWalletsRouter: ManageWalletsModule.IRouter {

    val openRestoreModule = SingleLiveEvent<PredefinedAccountType>()
    val closeLiveDate = SingleLiveEvent<Void>()
    val showCoinSettings = SingleLiveEvent<Unit>()

    override fun showCoinSettings() {
        showCoinSettings.call()
    }

    override fun openRestore(predefinedAccountType: PredefinedAccountType) {
        openRestoreModule.postValue(predefinedAccountType)
    }

    override fun close() {
        closeLiveDate.call()
    }
}
