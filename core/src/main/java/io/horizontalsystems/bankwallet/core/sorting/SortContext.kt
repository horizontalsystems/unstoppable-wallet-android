package io.horizontalsystems.bankwallet.core.sorting

import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

open class SortContext(val filter: String = "")

class TokenSortContext(
    filter: String = "",
    val enabledTokens: Set<Token> = emptySet(),
    val fiatValues: Map<Token, BigDecimal> = emptyMap(),
    val balanceOrder: Map<Token, Int> = emptyMap(),
) : SortContext(filter)

class FullCoinSortContext(
    filter: String = "",
    val fiatValues: Map<FullCoin, BigDecimal> = emptyMap(),
    val activeCoins: Set<FullCoin> = emptySet()
) : SortContext(filter)
