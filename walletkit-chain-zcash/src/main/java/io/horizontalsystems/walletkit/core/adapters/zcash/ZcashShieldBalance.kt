package io.horizontalsystems.walletkit.core.adapters.zcash

import java.math.BigDecimal

/** What a send or swap form must wait for or start before it can spend an amount of ZEC. */
sealed class ZcashSpendStep {
    /** Shielding the transparent funds covers the amount. */
    data object Shield : ZcashSpendStep()

    /** A shielding is on its way. */
    data object Shielding : ZcashSpendStep()

    /** Shielded funds still confirming cover the amount; no transparent funds are needed. */
    data object Confirming : ZcashSpendStep()
}

/**
 * The wallet's ZEC as a send or swap form sees it. Shared with iOS
 * (`ZcashShieldHelper`), so both apps offer the same balance and the same steps.
 *
 * @param available shielded funds a proposal can spend now
 * @param full all shielded funds, including unconfirmed incoming, change and shielding output
 * @param transparent transparent funds, spendable only once shielded
 * @param shielding whether a shielding is in flight
 * @param heldOutput output of the newest shielding while it is short of the anchor depth
 */
data class ZcashShieldBalance(
    val available: BigDecimal,
    val full: BigDecimal,
    val transparent: BigDecimal,
    val threshold: BigDecimal,
    val shielding: Boolean = false,
    val heldOutput: BigDecimal = BigDecimal.ZERO,
) {
    /** Transparent funds a shielding takes; at or below the threshold the SDK takes none. */
    val shieldable: BigDecimal
        get() = if (transparent > threshold) transparent else BigDecimal.ZERO

    /**
     * The part of [heldOutput] the wallet summary already counts as available. The summary
     * counts a shielding's output once its transparent inputs are deep enough, but a proposal
     * spends it only once the shielding itself is behind the anchor, 3 blocks deep. Until it
     * is mined the output is still pending, so nothing is held.
     */
    val heldBack: BigDecimal
        get() = (heldOutput - (full - available).max(BigDecimal.ZERO))
            .coerceIn(BigDecimal.ZERO, available.max(BigDecimal.ZERO))

    /** What a transfer proposal can spend now. */
    val spendable: BigDecimal
        get() = available - heldBack

    /**
     * What the user can spend now, once their funds confirm, or after shielding. One number for
     * every form; its fee and the shielding fee are left to the proposal, as on other chains.
     * The max covers a cached available above full under the SDK's balance mask.
     */
    val balance: BigDecimal
        get() = full.max(available) + shieldable

    /** The step [amount] needs, or null when it can be spent now or exceeds [balance]. */
    fun step(amount: BigDecimal): ZcashSpendStep? {
        return when {
            amount <= spendable -> null
            // Its transparent inputs are spent already, so the shielding is waited out even
            // when new transparent funds have arrived since.
            shielding && amount <= balance -> ZcashSpendStep.Shielding
            // Unconfirmed funds cover it; shielding is offered only when they will not.
            amount <= full.max(available) -> ZcashSpendStep.Confirming
            amount <= balance -> ZcashSpendStep.Shield
            else -> null
        }
    }

    companion object {
        /** Depth after which the SDK reports a transaction Confirmed. */
        const val CONFIRMATIONS_THRESHOLD = 10

        /** Depth after which a proposal spends a shielding's output. */
        const val ANCHOR_CONFIRMATIONS = 3

        /** A transaction mined in the latest block has 1 confirmation. */
        fun confirmations(minedHeight: Long, lastBlockHeight: Long): Int =
            (lastBlockHeight + 1 - minedHeight).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

        /**
         * Whether a shielding mined at [minedHeight] is still short of Confirmed; false while the
         * chain tip is unknown. No progress is shown: its output is spendable after 3 to 10
         * blocks depending on the age of its transparent inputs, which the SDK does not expose.
         */
        fun isShieldingInFlight(minedHeight: Long?, lastBlockHeight: Long): Boolean {
            if (lastBlockHeight <= 0) return false
            if (minedHeight == null) return true
            return confirmations(minedHeight, lastBlockHeight) < CONFIRMATIONS_THRESHOLD
        }

        /** Whether a shielding mined at [minedHeight] has its output counted but not spendable. */
        fun isHeld(minedHeight: Long?, lastBlockHeight: Long): Boolean {
            if (minedHeight == null || lastBlockHeight <= 0) return false
            return confirmations(minedHeight, lastBlockHeight) in 1 until ANCHOR_CONFIRMATIONS
        }
    }
}

/** A transaction the wallet sent: a transfer, a migration or a shielding. */
data class ZcashOwnTransaction(
    val txId: String,
    val isShielding: Boolean,
    val minedHeight: Long?,
    val expiryHeight: Long?,
    val timestamp: Long,
    /** What the transaction brought back to the wallet; for a shielding, its output. */
    val received: BigDecimal,
    /** Reported expired by the SDK when the list was read. */
    val expired: Boolean = false,
) {
    /** Unmined past its expiry height; 0 disables expiry. */
    fun isExpired(lastBlockHeight: Long): Boolean {
        if (expired) return true
        val expiryHeight = expiryHeight ?: return false
        return minedHeight == null && expiryHeight > 0 && expiryHeight <= lastBlockHeight
    }

    companion object {
        /**
         * The newest of [transactions] that can still be mined: unmined ones first, ties
         * broken by expiry height, then time.
         */
        fun latest(transactions: List<ZcashOwnTransaction>, lastBlockHeight: Long): ZcashOwnTransaction? =
            transactions
                .filterNot { it.isExpired(lastBlockHeight) }
                .maxWithOrNull(
                    compareBy<ZcashOwnTransaction> { it.minedHeight ?: Long.MAX_VALUE }
                        .thenBy { it.expiryHeight ?: 0L }
                        .thenBy { it.timestamp }
                )
    }
}
