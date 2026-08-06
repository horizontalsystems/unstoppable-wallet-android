package io.horizontalsystems.walletkit.entities.transactionrecords.solana

import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.Token

class SolanaUnknownTransactionRecord(
        transaction: SolanaTransactionInfo,
        baseToken: Token,
        source: TransactionSource,
        val incomingTransfers: List<Transfer>,
        val outgoingTransfers: List<Transfer>,
        spam: Boolean = false,
): SolanaTransactionRecord(transaction, baseToken, source, spam)
