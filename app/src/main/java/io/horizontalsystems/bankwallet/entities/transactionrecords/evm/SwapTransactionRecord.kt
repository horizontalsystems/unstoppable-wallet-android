package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.PlatformCoin

class SwapTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: PlatformCoin,
    // valueIn stores amountInMax in cases when exact valueIn amount is not known
    val valueIn: TransactionValue,
    // valueOut stores amountOutMin in cases when exact valueOut amount is not known
    val valueOut: TransactionValue?,
    val exchangeAddress: String,
    val foreignRecipient: Boolean,
    source: TransactionSource
) : EvmTransactionRecord(fullTransaction, baseCoin, source)
