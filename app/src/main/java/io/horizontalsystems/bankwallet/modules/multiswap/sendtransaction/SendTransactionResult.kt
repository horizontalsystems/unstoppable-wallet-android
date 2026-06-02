package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.ethereumkit.models.FullTransaction

sealed class SendTransactionResult {
    data class Evm(val fullTransaction: FullTransaction) : SendTransactionResult()
    data class Btc(val transactionRecord: BitcoinTransactionRecord?) : SendTransactionResult()
    data class Tron(val txHash: String? = null) : SendTransactionResult()
    object Stellar : SendTransactionResult()
    data class Solana(val txHash: String? = null) : SendTransactionResult()
    object Ton : SendTransactionResult()
    data class Zcash(val transactionHash: String?) : SendTransactionResult()
    data class Monero(val txHash: String? = null) : SendTransactionResult()
    data class Zano(val txHash: String? = null) : SendTransactionResult()
}
