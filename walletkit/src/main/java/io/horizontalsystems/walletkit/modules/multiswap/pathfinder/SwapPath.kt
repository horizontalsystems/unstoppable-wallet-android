package io.horizontalsystems.walletkit.modules.multiswap.pathfinder

import io.horizontalsystems.marketkit.models.Token

/**
 * A two-leg route through one intermediate token, suggested when the pair can't be swapped
 * directly. Only provider support for each leg is checked, never a quote, so the route is a hint
 * the user may try, not a promise.
 */
data class SwapPath(
    val tokenIn: Token,
    val intermediateToken: Token,
    val tokenOut: Token,
)
