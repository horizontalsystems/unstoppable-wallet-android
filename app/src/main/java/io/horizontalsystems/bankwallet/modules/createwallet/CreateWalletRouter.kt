package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinSettings

class CreateWalletRouter : CreateWalletModule.IRouter {
    val startMainModuleLiveEvent = SingleLiveEvent<Unit>()
    val close = SingleLiveEvent<Unit>()
    val showCoinSettings = SingleLiveEvent<Pair<Coin, CoinSettings>>()

    override fun startMainModule() {
        startMainModuleLiveEvent.call()
    }

    override fun showCoinSettings(coin: Coin, coinSettingsToRequest: CoinSettings) {
        showCoinSettings.postValue(Pair(coin, coinSettingsToRequest))
    }

    override fun close() {
        close.call()
    }
}
