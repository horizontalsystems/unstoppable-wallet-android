package io.horizontalsystems.stellarkit.room

sealed class StellarAsset {
    data object Native : StellarAsset()
    data class Asset(val code: String, val issuer: String) : StellarAsset()

    val id by lazy {
        when (this) {
            Native -> "native"
            is Asset -> "$code:$issuer"
        }
    }

    companion object {
        fun fromSdkModel(asset: org.stellar.sdk.Asset) = when (asset) {
            is org.stellar.sdk.AssetTypeNative -> Native
            is org.stellar.sdk.AssetTypeCreditAlphaNum -> Asset(asset.code, asset.issuer)
            else -> throw IllegalStateException("")
        }

        fun fromId(id: String): StellarAsset {
            if (id == "native") return Native

            val (code, issuer) = id.split(":")
            return Asset(code, issuer)
        }
    }
}
