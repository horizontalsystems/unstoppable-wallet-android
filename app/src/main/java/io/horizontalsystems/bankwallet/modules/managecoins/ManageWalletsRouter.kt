package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class ManageWalletsRouter: ManageWalletsModule.IRouter {

    val openRestoreModule = SingleLiveEvent<PredefinedAccountType>()
    val closeLiveDate = SingleLiveEvent<Void>()
    val showCoinSettings = SingleLiveEvent<Triple<Coin, CoinSettings, AccountOrigin>>()

    override fun showCoinSettings(coin: Coin, coinSettingsToRequest: CoinSettings, origin: AccountOrigin) {
        showCoinSettings.postValue(Triple(coin, coinSettingsToRequest, origin))
    }

    override fun openRestore(predefinedAccountType: PredefinedAccountType) {
        openRestoreModule.postValue(predefinedAccountType)
    }

    override fun close() {
        closeLiveDate.call()
    }
}
