package cash.p.terminal.core.utils

import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.SwapProviderTransaction
import java.math.BigDecimal

/**
 * Represents an incoming transaction to be matched with a swap.
 */
data class IncomingTransaction(
    val uid: String,
    val amount: BigDecimal?,
    val timestamp: Long,  // in milliseconds
    val coinUid: String,
    val blockchainType: String,
    val addresses: List<String>?,
    val accountId: String
)

/**
 * Matches incoming transactions to swap operations.
 * Handles address-based matching, amount+timestamp matching, and fallback timestamp matching.
 */
class SwapTransactionMatcher(
    private val storage: SwapProviderTransactionsStorage
) {

    companion object {
        const val TIME_WINDOW_MS = 10_800_000L // 3 hours
        const val AMOUNT_TOLERANCE = 0.005 // 0.5%
    }

    /**
     * Finds a matching swap for an incoming transaction.
     *
     * Matching priority:
     * 1. Already matched (cached by incomingRecordUid)
     * 2. Address + amount matching (when addresses available)
     * 3. Amount + timestamp matching (when addresses not available)
     * 4. Fallback to timestamp-only matching
     *
     * @param transaction The incoming transaction to match
     * @return The matched SwapProviderTransaction or null if no match found
     */
    fun findMatchingSwap(transaction: IncomingTransaction): SwapProviderTransaction? {
        // 1. Check if already matched
        storage.getByIncomingRecordUid(transaction.uid)?.let { return it }

        val amount = transaction.amount
        val addresses = transaction.addresses

        // 2. Try address + amount matching
        val matchedSwap = if (!addresses.isNullOrEmpty() && amount != null) {
            addresses.firstNotNullOfOrNull { address ->
                storage.getByAddressAndAmount(
                    address = address,
                    blockchainType = transaction.blockchainType,
                    coinUid = transaction.coinUid,
                    amount = amount,
                    timestamp = transaction.timestamp
                )
            }?.also { swap ->
                storage.setIncomingRecordUid(
                    date = swap.date,
                    incomingRecordUid = transaction.uid,
                    amountOutReal = amount
                )
            }
        } else if (amount != null) {
            // 3. Amount + timestamp matching (no addresses)
            // Query returns results ordered by closest amount match, then by date
            storage.getUnmatchedSwapsByTokenOut(
                coinUid = transaction.coinUid,
                blockchainType = transaction.blockchainType,
                fromTimestamp = transaction.timestamp - TIME_WINDOW_MS,
                toTimestamp = transaction.timestamp,
                amount = amount,
                tolerance = AMOUNT_TOLERANCE,
                accountId = transaction.accountId
            ).firstOrNull()?.also { swap ->
                storage.setIncomingRecordUid(
                    date = swap.date,
                    incomingRecordUid = transaction.uid,
                    amountOutReal = amount
                )
            }
        } else {
            null
        }

        // 4. Fallback to timestamp-only matching
        return matchedSwap ?: storage.getByTokenOut(
            coinUid = transaction.coinUid,
            blockchainType = transaction.blockchainType,
            timestamp = transaction.timestamp,
            accountId = transaction.accountId
        )
    }

}
