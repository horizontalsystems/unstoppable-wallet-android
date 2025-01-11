package cash.p.terminal.entities.transactionrecords.solana

import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.solanakit.models.Transaction

class SolanaUnknownTransactionRecord(
    transaction: Transaction,
    baseToken: Token,
    source: TransactionSource,
    val incomingTransfers: List<Transfer>,
    val outgoingTransfers: List<Transfer>
): SolanaTransactionRecord(transaction, baseToken, source)
