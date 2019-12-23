package io.horizontalsystems.bankwallet.modules.coinsettings

import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinSettings

class CoinSettingsRouter: CoinSettingsModule.IRouter {

    val notifyOptionsLiveEvent = SingleLiveEvent<Pair<Coin, CoinSettings>>()
    val onCancelClick = SingleLiveEvent<Unit>()

    override fun notifyOptions(coinSettings: CoinSettings, coin: Coin) {
        notifyOptionsLiveEvent.postValue(Pair(coin, coinSettings))
    }

    override fun onCancelClick() {
        onCancelClick.call()
    }
}