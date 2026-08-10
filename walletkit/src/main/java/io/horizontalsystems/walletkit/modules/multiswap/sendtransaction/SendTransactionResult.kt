package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinTransactionRecord

sealed class SendTransactionResult {
    data class Evm(val transactionHash: String?) : SendTransactionResult()
    data class Btc(val transactionRecord: BitcoinTransactionRecord?) : SendTransactionResult()
    data class Tron(val txHash: String? = null) : SendTransactionResult()
    data class Stellar(val txHash: String? = null) : SendTransactionResult()
    data class Solana(val txHash: String? = null) : SendTransactionResult()
    object Ton : SendTransactionResult()
    data class Zcash(val transactionHash: String?) : SendTransactionResult()
    data class Monero(val txHash: String? = null) : SendTransactionResult()
    data class Thorchain(val txHash: String? = null) : SendTransactionResult()
    data class Zano(val txHash: String? = null) : SendTransactionResult()
}
