package bitcoin.wallet.entities

import com.google.gson.annotations.SerializedName

enum class CurrencyType (val rawValue: String) {
    @SerializedName("DIGITAL")
    DIGITAL("DIGITAL"),

    @SerializedName("FIAT")
    FIAT("FIAT");

    val isDigital: Boolean
        get() = this == DIGITAL

    val isFiat: Boolean
        get() = this == FIAT


    companion object {
        private val map = values().associateBy(CurrencyType::rawValue)
        fun fromRawValue(rawValue: String) = map[rawValue]
    }
}
