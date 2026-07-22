package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * Caller-supplied allowance policy for a swap screen. A true verdict means no
 * separate approve/revoke step should be surfaced for this swap — e.g. smart
 * accounts that batch the approve into their own swap operation, where the
 * execution layer owns the allowance. Passed into [SwapViewModel.Factory];
 * absent policy keeps the stock behavior.
 */
interface ISwapAllowancePolicy {
    fun allowanceNotRequired(token: Token, amountIn: BigDecimal): Boolean
}
