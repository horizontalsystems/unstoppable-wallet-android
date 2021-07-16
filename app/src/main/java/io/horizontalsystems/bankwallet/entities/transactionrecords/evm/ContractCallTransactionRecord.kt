package io.horizontalsystems.bankwallet.entities.transactionrecords.evm

import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.models.FullTransaction

class ContractCallTransactionRecord(
    fullTransaction: FullTransaction,
    baseCoin: Coin,
    val contractAddress: String,
    val method: String?,
    override val foreignTransaction: Boolean = false
) : EvmTransactionRecord(fullTransaction, baseCoin)
