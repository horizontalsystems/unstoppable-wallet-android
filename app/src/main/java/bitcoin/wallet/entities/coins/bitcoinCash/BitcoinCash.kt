package bitcoin.wallet.entities.coins.bitcoinCash

import bitcoin.wallet.entities.coins.Coin

class BitcoinCash() : Coin() {
    private var suffix: String = ""

    constructor(suffix: String) : this() {
        this.suffix = suffix
    }

    override val name: String
        get() = if (suffix.isNotEmpty()) {
            "Bitcoin Cash-$suffix"
        } else {
            "Bitcoin Cash"
        }

    override val code: String
        get() = if (suffix.isNotEmpty()) {
            "BCH-$suffix"
        } else {
            "BCH"
        }

}
