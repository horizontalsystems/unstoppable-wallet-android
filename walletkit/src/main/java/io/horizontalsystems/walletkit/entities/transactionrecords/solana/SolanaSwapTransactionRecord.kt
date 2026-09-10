package io.horizontalsystems.walletkit.entities.transactionrecords.solana

import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

// A DEX swap detected via the transaction's recognized program ids (SolanaKit `KnownPrograms`,
// e.g. Jupiter) — the Solana counterpart of the EVM `SwapTransactionRecord`. `valueIn` (paid) /
// `valueOut` (received) are null while the transaction is still pending: the kit's pending record
// carries no balance changes yet, but the program id is known at send time, so the row can already
// render as "Swap / <exchange>" instead of an unknown transaction.
//
// `tokenIn` / `tokenOut` name the swapped pair independently of the legs: a 1inch Fusion swap is
// split across an order-create (only the paid side moves) and a resolver's fill (only the received
// side moves), so one of the values is null on each of the two transactions even once confirmed —
// yet both instructions name the pair, which the kit surfaces. A side's token is null only when
// neither its leg nor the instruction names it (e.g. a cross-chain LI.FI swap's far side).
class SolanaSwapTransactionRecord(
        transaction: SolanaTransactionInfo,
        baseToken: Token,
        source: TransactionSource,
        val exchangeName: String,
        val valueIn: TransactionValue?,
        val valueOut: TransactionValue?,
        val tokenIn: Token?,
        val tokenOut: Token?,
): SolanaTransactionRecord(transaction, baseToken, source) {

    override val mainValue = valueOut

}
