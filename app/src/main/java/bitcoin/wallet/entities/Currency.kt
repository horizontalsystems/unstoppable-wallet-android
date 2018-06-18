package bitcoin.wallet.entities

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

data class CurrencyValue(val currency: Currency, val value: Double)

abstract class Coin {
    abstract val name: String
    abstract val code: String

    override fun equals(other: Any?): Boolean {
        if (other is Coin) {
            return name == other.name && code == other.code
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + code.hashCode()
        return result
    }

}

class Bitcoin : Coin() {
    override val name: String = "Bitcoin"
    override val code: String = "BTC"
}

class BitcoinCash : Coin() {
    override val name: String = "Bitcoin Cash"
    override val code: String = "BCH"
}

class Ethereum : Coin() {
    override val name: String = "Ethereum"
    override val code: String = "ETH"
}

data class CoinValue(val coin: Coin, val value: Double) {

    override fun equals(other: Any?): Boolean {
        if (other is CoinValue) {
            return coin == other.coin && value == other.value

        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = coin.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

}

data class WalletBalanceItem(val coinValue: CoinValue, val exchangeRate: Double, val currency: Currency)

data class WalletBalanceViewItem(val coinValue: CoinValue, val exchangeValue: CurrencyValue, val currencyValue: CurrencyValue) {

    override fun equals(other: Any?): Boolean {
        if (other is WalletBalanceViewItem) {
            return coinValue == other.coinValue &&
                    exchangeValue == other.exchangeValue &&
                    currencyValue == other.currencyValue
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = coinValue.hashCode()
        result = 31 * result + exchangeValue.hashCode()
        result = 31 * result + currencyValue.hashCode()
        return result
    }

}
