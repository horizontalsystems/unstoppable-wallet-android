package bitcoin.wallet.entities.coins.ethereum

import bitcoin.wallet.entities.coins.Coin

class Ethereum : Coin() {
    override val name: String = "Ethereum"
    override val code: String = "ETH"
}
