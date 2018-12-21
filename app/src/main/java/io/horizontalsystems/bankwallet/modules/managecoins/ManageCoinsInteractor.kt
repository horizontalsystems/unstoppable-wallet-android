package io.horizontalsystems.bankwallet.modules.managecoins

import android.util.Log
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class ManageCoinsInteractor : ManageCoinsModule.IInteractor {

    var delegate: ManageCoinsModule.IInteractorDelegate? = null

    override fun loadCoins() {
        val allCoins = mutableListOf(
                Coin("Bitcoin", "BTC", CoinType.Bitcoin),
                Coin("Ethereum", "ETH", CoinType.Ethereum),
                Coin("DASH Coin", "DASH", CoinType.Ethereum),
                Coin("Litecoin", "LTC", CoinType.BitcoinCash),
                Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash)
        )
        val enabledCoins = mutableListOf(
                Coin("Bitcoin", "BTC", CoinType.Bitcoin),
                Coin("XRP", "XRP", CoinType.Ethereum),
                Coin("Litecoin", "LTC", CoinType.BitcoinCash))
        delegate?.didLoadCoins(allCoins, enabledCoins)
    }

    override fun saveEnabledCoins(enabledCoins: List<Coin>) {
        Log.e("ManageCoinsInter","Enabled Coins save")
    }

}
