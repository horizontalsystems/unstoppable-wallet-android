package io.horizontalsystems.marketkit.models

data class TokenResponse(
    val coin_uid: String,
    val blockchain_uid: String,
    val type: String,
    val decimals: Int?,
    val address: String?,
    val symbol: String?
)
