package io.horizontalsystems.marketkit.models

import java.math.BigDecimal

data class NftPrice(
    val token: Token,
    val value: BigDecimal
)
