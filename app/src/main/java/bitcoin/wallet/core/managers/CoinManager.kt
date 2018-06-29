package bitcoin.wallet.core.managers

import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCash

class CoinManager {

    fun getCoinByCode(code: String) : Coin? = when (code) {
        "BTC" -> Bitcoin()
        "BCH" -> BitcoinCash()
        else -> null
    }

}
