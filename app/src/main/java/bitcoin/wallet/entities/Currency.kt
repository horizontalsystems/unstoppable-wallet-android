package bitcoin.wallet.entities

import com.google.gson.annotations.SerializedName

class Currency {
    var code: String = ""

    @SerializedName("symbol_ucode")
    var symbol: String? = null

    @SerializedName("display_name")
    var name: String = ""

    var type: CurrencyType = CurrencyType.FIAT

    @SerializedName("code_numeric")
    var codeNumeric: Int = 0

    override fun equals(other: Any?): Boolean {
        if (other is Currency) {
            return code == other.code
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + (symbol?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + codeNumeric
        return result
    }

    fun getSymbolChar(): Char? {
        val hex = symbol?.replace("U+", "")
        var symbolChar: Char? = null
        hex?.let {
            symbolChar = Integer.parseInt(it, 16).toChar()
        }
        return symbolChar
    }

}
