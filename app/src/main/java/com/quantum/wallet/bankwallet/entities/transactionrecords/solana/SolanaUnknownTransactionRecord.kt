package com.quantum.wallet.bankwallet.entities.transactionrecords.solana

import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
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
