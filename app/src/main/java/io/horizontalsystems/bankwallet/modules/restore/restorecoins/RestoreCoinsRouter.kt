package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.SingleLiveEvent

class RestoreCoinsRouter : RestoreCoinsModule.IRouter {
    val close = SingleLiveEvent<Unit>()
    val closeWithSelectedCoins = SingleLiveEvent<List<Coin>>()
    val showBlockchainSettingsListEvent = SingleLiveEvent<List<CoinType>>()

    override fun close() {
        close.call()
    }

    override fun showBlockchainSettingsList(coinTypes: List<CoinType>) {
        showBlockchainSettingsListEvent.postValue(coinTypes)
    }

    override fun closeWithSelectedCoins(enabledCoins: MutableList<Coin>) {
        closeWithSelectedCoins.postValue(enabledCoins)
    }
}
