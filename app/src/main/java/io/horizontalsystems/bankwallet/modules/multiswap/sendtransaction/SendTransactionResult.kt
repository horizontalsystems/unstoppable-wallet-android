package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.ethereumkit.models.FullTransaction

sealed class SendTransactionResult {
    data class Evm(val fullTransaction: FullTransaction) : SendTransactionResult()
    data class Btc(val transactionRecord: BitcoinTransactionRecord?) : SendTransactionResult()
    object Tron : SendTransactionResult()
    object Stellar : SendTransactionResult()
    object Solana : SendTransactionResult()
}
