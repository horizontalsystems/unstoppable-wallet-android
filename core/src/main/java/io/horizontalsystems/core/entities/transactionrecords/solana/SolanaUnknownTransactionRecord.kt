package io.horizontalsystems.core.entities.transactionrecords.solana

import io.horizontalsystems.core.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.solanakit.models.Transaction

class SolanaUnknownTransactionRecord(
        transaction: Transaction,
        baseToken: Token,
        source: TransactionSource,
        val incomingTransfers: List<Transfer>,
        val outgoingTransfers: List<Transfer>,
        spam: Boolean = false,
): SolanaTransactionRecord(transaction, baseToken, source, spam)
