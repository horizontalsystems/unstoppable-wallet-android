package io.horizontalsystems.bankwallet.modules.managecoins

import android.util.Log
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class ManageCoinsInteractor : ManageCoinsModule.IInteractor {

    var delegate: ManageCoinsModule.IInteractorDelegate? = null

    override fun loadCoins() {
        val enabledCoins = mutableListOf(Coin("Bitcoin", "BTC", CoinType.Bitcoin), Coin("Ethereum", "ETH", CoinType.Ethereum))
        val disabledCoins = mutableListOf(Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash))
        delegate?.didLoadCoins(enabledCoins, disabledCoins)
    }

    override fun saveEnabledCoins(enabledCoins: List<Coin>) {
        Log.e("ManageCoinsInter","Enabled Coins save")
    }

}
