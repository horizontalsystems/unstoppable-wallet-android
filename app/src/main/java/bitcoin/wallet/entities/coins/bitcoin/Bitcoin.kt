package bitcoin.wallet.entities.coins.bitcoin

import bitcoin.wallet.entities.coins.Coin

class Bitcoin : Coin() {
    override val name: String = "Bitcoin"
    override val code: String = "BTC"
}
