package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class CoinSettingType : Parcelable {
    derivation,
    bitcoinCashCoinType
}

@Parcelize
class CoinSettings(val settings: Map<CoinSettingType, String> = mapOf()) : Parcelable {
    constructor(id: String) : this(fromId(id))

    val id: String
        get() = settings.map { (t, u) ->
            "${t.name}:$u"
        }.joinToString("|")

    val derivation: AccountType.Derivation?
        get() = settings[CoinSettingType.derivation]?.let {
            AccountType.Derivation.fromString(it)
        }

    val bitcoinCashCoinType: BitcoinCashCoinType?
        get() = settings[CoinSettingType.bitcoinCashCoinType]?.let {
            BitcoinCashCoinType.fromString(it)
        }

    override fun equals(other: Any?): Boolean {
        if (other !is CoinSettings) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {

        private fun fromId(id: String): Map<CoinSettingType, String> {
            return id.split("|").mapNotNull {
                val parts = it.split(":")
                if (parts.size != 2) return@mapNotNull null

                val (key, value) = parts

                CoinSettingType.valueOf(key) to value
            }.toMap()
        }
    }
}
