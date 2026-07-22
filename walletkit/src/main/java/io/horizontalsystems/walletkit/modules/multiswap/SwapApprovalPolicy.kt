package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

/**
 * Registration seam for externally-supplied allowance policies. A true verdict
 * means no separate approve/revoke step is required for this (spender, token,
 * amount) — e.g. smart accounts batching the approve into their own swap
 * operation, where the execution layer owns the allowance. A false verdict
 * (or no provider) falls through to the stock on-chain allowance check.
 */
object SwapApprovalPolicy {

    interface Provider {
        fun allowanceNotRequired(spenderAddress: Address, token: Token, amountIn: BigDecimal): Boolean
    }

    @Volatile
    var provider: Provider? = null
}
