package io.horizontalsystems.bankwallet.modules.configuredtoken

sealed class ConfiguredTokenInfoType {
    data class Contract(
        val reference: String,
        val platformImageUrl: String,
        val explorerUrl: String?
    ) : ConfiguredTokenInfoType()

    object Bips: ConfiguredTokenInfoType()
    object Bch: ConfiguredTokenInfoType()
    data class BirthdayHeight(val height: Long?): ConfiguredTokenInfoType()
}