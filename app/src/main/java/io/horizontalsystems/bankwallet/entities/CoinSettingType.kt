package io.horizontalsystems.bankwallet.entities

enum class CoinSettingType {
    derivation,
    bitcoinCashCoinType
}

class CoinSettings(private val settings: Map<CoinSettingType, String> = mapOf()) {

    val id: String
        get() = settings.map { (t, u) ->
            "${t.name}:$u"
        }.joinToString("|")
}
