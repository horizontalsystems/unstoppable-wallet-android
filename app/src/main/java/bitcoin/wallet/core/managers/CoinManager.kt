package bitcoin.wallet.core.managers

import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCash

class CoinManager {

    private val supportedCoins = listOf(Bitcoin(), BitcoinCash())

    fun getCoinByCode(code: String): Coin? =
            supportedCoins.firstOrNull { it.code == code }

}
