package io.horizontalsystems.walletkit.core

import androidx.annotation.StringRes
import java.math.BigDecimal

/**
 * A token the account must opt into before it can receive it: a Stellar trustline, an XRPL
 * trust line. The shared receive and activation screens drive any adapter implementing this.
 */
interface IActivatableTokenAdapter : IReceiveAdapter {
    /** Network fee of the activation transaction, in the chain's native coin. */
    val activationFee: BigDecimal

    /** True once the account can receive the token. */
    suspend fun isActivated(): Boolean

    /** Throws [TokenActivationError] when the account cannot activate right now. */
    fun validateActivation()

    suspend fun activate()
}

sealed class TokenActivationError : Exception() {
    class InsufficientBalance : TokenActivationError()
}

/** Chain-specific wording for the shared activation flow. */
class TokenActivationInfo(
    /** Explains why activation is needed; receives the coin code twice as format arguments. */
    @StringRes val dialogDescriptionRes: Int,
    @StringRes val insufficientBalanceDescriptionRes: Int,
    /** Optional note shown on the confirmation page, e.g. the reserve an XRPL trust line locks. */
    val reserveNote: String? = null,
)
