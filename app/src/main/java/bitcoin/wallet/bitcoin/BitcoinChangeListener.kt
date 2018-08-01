package bitcoin.wallet.bitcoin

import bitcoin.wallet.entities.TransactionRecord

interface BitcoinChangeListener {

    fun onBalanceChange(value: Long)
    fun onNewTransaction(tx: TransactionRecord)
    fun onTransactionConfidenceChange(tx: TransactionRecord)
    fun onBestChainHeightChange(value: Int)

}
