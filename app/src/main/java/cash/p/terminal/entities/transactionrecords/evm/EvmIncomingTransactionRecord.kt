package cash.p.terminal.entities.transactionrecords.evm

import cash.p.terminal.core.managers.SpamManager
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.ethereumkit.models.Transaction

class EvmIncomingTransactionRecord(
    transaction: Transaction,
    val baseToken: Token,
    source: TransactionSource,
    spamManager: SpamManager,
    val from: String,
    val value: TransactionValue
) : EvmTransactionRecord(
    transaction = transaction,
    baseToken = baseToken,
    source = source,
    foreignTransaction = true,
    spam = spamManager.isIncomingSpam(value)
) {

    override val mainValue = value

}
