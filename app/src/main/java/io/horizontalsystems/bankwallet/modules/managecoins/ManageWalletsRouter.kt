package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class ManageWalletsRouter: ManageWalletsModule.IRouter {

    val openRestoreModule = SingleLiveEvent<PredefinedAccountType>()
    val showBlockchainSettings = SingleLiveEvent<CoinType>()
    val closeLiveDate = SingleLiveEvent<Void>()

    override fun openRestore(predefinedAccountType: PredefinedAccountType) {
        openRestoreModule.postValue(predefinedAccountType)
    }

    override fun showSettings(coinType: CoinType) {
        showBlockchainSettings.postValue(coinType)
    }

    override fun close() {
        closeLiveDate.call()
    }
}
