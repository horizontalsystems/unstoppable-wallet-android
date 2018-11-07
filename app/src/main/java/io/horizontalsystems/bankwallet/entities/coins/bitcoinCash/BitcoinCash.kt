package io.horizontalsystems.bankwallet.entities.coins.bitcoinCash

import io.horizontalsystems.bankwallet.entities.coins.Coin

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
