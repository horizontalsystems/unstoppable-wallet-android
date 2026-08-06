package io.horizontalsystems.walletkit.entities.transactionrecords.solana

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

// A DEX swap detected via the transaction's recognized program ids (SolanaKit `KnownPrograms`,
// e.g. Jupiter) — the Solana counterpart of the EVM `SwapTransactionRecord`. `valueIn` (paid) /
// `valueOut` (received) are null while the transaction is still pending: the kit's pending record
// carries no balance changes yet, but the program id is known at send time, so the row can already
// render as "Swap / <exchange>" instead of an unknown transaction.
class SolanaSwapTransactionRecord(
        transaction: SolanaTransactionInfo,
        baseToken: Token,
        source: TransactionSource,
        val exchangeName: String,
        val valueIn: TransactionValue?,
        val valueOut: TransactionValue?,
): SolanaTransactionRecord(transaction, baseToken, source) {

    override val mainValue = valueOut

}
