package bitcoin.wallet.entities.coins.bitcoin

import bitcoin.wallet.entities.coins.Coin

class Bitcoin() : Coin() {

    var suffix: String = ""

    constructor(suffix: String) : this() {
        this.suffix = suffix
    }

    override val name: String = if (suffix.isNotEmpty()) {
        "Bitcoin-$suffix"
    } else {
        "Bitcoin"
    }

    override val code: String = if (suffix.isNotEmpty()) {
        "BTC-$suffix"
    } else {
        "BTC"
    }

}
