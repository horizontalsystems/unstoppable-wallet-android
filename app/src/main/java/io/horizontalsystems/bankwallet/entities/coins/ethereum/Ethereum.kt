package io.horizontalsystems.bankwallet.entities.coins.ethereum

import io.horizontalsystems.bankwallet.entities.coins.Coin

class Ethereum : Coin() {
    override val name: String = "Ethereum"
    override val code: String = "ETH"
}
