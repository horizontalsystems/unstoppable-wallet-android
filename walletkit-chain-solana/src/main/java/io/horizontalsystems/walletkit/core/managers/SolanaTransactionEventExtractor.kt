package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.solanakit.models.FullTransaction

/**
 * Extracts counterparty context from Solana transactions for address-poisoning correlation
 * (see [PoisoningScorer] / [SpamManager]).
 *
 * Correlation must run against every address the user has legitimately transacted with — in
 * BOTH directions. Real Solana poisoning routinely mimics the sender of an INCOMING payment
 * (e.g. whoever just sent you a large amount of SOL) so that, when you later scroll your history
 * and copy "the address that paid me", you copy the look-alike instead. An outgoing-only context
 * can never catch that — and a watch/receive-only wallet has no outgoing history at all.
 *
 * The counterparty is therefore taken direction-agnostically: the sender (`transaction.from`) of a
 * received transfer, or the recipient (`transaction.to`) of a sent one. Both are wallet-owner
 * addresses in the Solana kit's model (native SOL and SPL alike), which is what the attacker
 * vanity-generates and what prefix/suffix matching compares.
 *
 * The Solana kit stores no confirmed slot, so block correlation can never contribute for Solana;
 * only timestamp and address are available (blockHeight left null).
 */
class SolanaTransactionEventExtractor {

    /**
     * Map any user transaction to the counterparty the scorer compares incoming senders against,
     * regardless of direction. Returns null when there is no usable counterparty (missing address
     * or a self-transfer).
     */
    fun extractCounterpartyInfo(
        fullTransaction: FullTransaction,
        userAddress: String
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTransaction.transaction

        val counterparty = when {
            // Received: the counterparty is the sender.
            !tx.from.isNullOrBlank() && tx.from != userAddress -> tx.from
            // Sent: the counterparty is the recipient.
            !tx.to.isNullOrBlank() && tx.to != userAddress -> tx.to
            else -> null
        } ?: return null

        return PoisoningScorer.OutgoingTxInfo(
            recipientAddress = counterparty,
            timestamp = tx.timestamp,
            blockHeight = null
        )
    }
}
