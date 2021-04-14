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

    val id: String
        get() = settings.map { (t, u) ->
            "${t.name}:$u"
        }.joinToString("|")
}
