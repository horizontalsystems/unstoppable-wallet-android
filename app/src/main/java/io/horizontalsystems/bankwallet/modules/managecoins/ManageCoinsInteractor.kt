package io.horizontalsystems.bankwallet.modules.managecoins

import android.util.Log
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType

class ManageCoinsInteractor : ManageCoinsModule.IInteractor {

    var delegate: ManageCoinsModule.IInteractorDelegate? = null

    override fun loadCoins() {
        val allCoins = mutableListOf(
                Coin("Bitcoin", "BTC", false, CoinType.Bitcoin),
                Coin("Ethereum", "ETH", false, CoinType.Ethereum),
                Coin("DASH Coin", "DASH", false, CoinType.Ethereum),
                Coin("Litecoin", "LTC", false, CoinType.BitcoinCash),
                Coin("XRP", "XRP", false, CoinType.Ethereum),
                Coin("Bitcoin Cash", "BCH", false, CoinType.BitcoinCash)
        )
        val enabledCoins = mutableListOf(
                Coin("Bitcoin", "BTC", true, CoinType.Bitcoin),
                Coin("XRP", "XRP", true, CoinType.Ethereum),
                Coin("Litecoin", "LTC", true, CoinType.BitcoinCash))
        delegate?.didLoadCoins(allCoins, enabledCoins)
    }

    override fun saveEnabledCoins(enabledCoins: List<Coin>) {
        Log.e("ManageCoinsInter","Enabled Coins save")
        delegate?.didSaveChanges()
    }

}
