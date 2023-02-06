package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class TokenHolder(
    val address: String,
    val share: BigDecimal
)
