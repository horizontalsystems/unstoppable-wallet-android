package bitcoin.wallet.entities.coins.bitcoinCash

import bitcoin.wallet.entities.coins.Coin

class BitcoinCash : Coin() {
    override val name: String = "Bitcoin Cash"
    override val code: String = "BCH"
}