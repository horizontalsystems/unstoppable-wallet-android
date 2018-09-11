package bitcoin.wallet.entities

import bitcoin.wallet.entities.coins.Coin

abstract class Currency {
    abstract val code: String
    abstract val symbol: String

    override fun equals(other: Any?): Boolean {
        if (other is Currency) {
            return code == other.code
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + symbol.hashCode()
        return result
    }

}

class DollarCurrency : Currency() {
    override val code: String = "USD"
    override val symbol: String = "$"

}

class Ethereum : Coin() {
    override val name: String = "Ethereum"
    override val code: String = "ETH"
}

class Cardano : Coin() {
    override val name: String = "Cardano"
    override val code: String = "ADA"
}

class Dash : Coin() {
    override val name: String = "Dash"
    override val code: String = "DASH"
}

class Litecoin : Coin() {
    override val name: String = "Litecoin"
    override val code: String = "LTC"
}

class Monero : Coin() {
    override val name: String = "Monero"
    override val code: String = "XMR"
}

class Xrp : Coin() {
    override val name: String = "Xrp"
    override val code: String = "XRP"
}

class Zcash : Coin() {
    override val name: String = "Zcash"
    override val code: String = "ZEC"
}

class EOS : Coin() {
    override val name: String = "EOS"
    override val code: String = "EOS"
}

class Tether : Coin() {
    override val name: String = "Tether"
    override val code: String = "USDT"
}

class Stellar : Coin() {
    override val name: String = "Stellar"
    override val code: String = "XLM"
}
