package bitcoin.wallet.entities

class Currency {
    var code: String = ""
    var symbol: String = ""
    var description: String = ""

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
