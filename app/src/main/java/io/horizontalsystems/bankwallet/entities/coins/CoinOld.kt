package io.horizontalsystems.bankwallet.entities.coins

abstract class CoinOld {
    abstract val name: String
    abstract val code: String

    override fun equals(other: Any?): Boolean {
        if (other is CoinOld) {
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
