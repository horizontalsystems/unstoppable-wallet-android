package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestoreCoinsRouter : RestoreCoinsModule.IRouter {
    val startMainModuleLiveEvent = SingleLiveEvent<Unit>()
    val showRestoreEvent = SingleLiveEvent<PredefinedAccountType>()
    val showCoinSettings = SingleLiveEvent<Pair<Coin, CoinSettings>>()
    val close = SingleLiveEvent<Unit>()

    override fun startMainModule() {
        startMainModuleLiveEvent.call()
    }

    override fun showCoinSettings(coin: Coin, coinSettingsToRequest: CoinSettings) {
        showCoinSettings.postValue(Pair(coin, coinSettingsToRequest))
    }

    override fun showRestore(predefinedAccountType: PredefinedAccountType) {
        showRestoreEvent.postValue(predefinedAccountType)
    }

    override fun close() {
        close.call()
    }
}
