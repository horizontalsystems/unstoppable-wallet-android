package bitcoin.wallet.bitcoin

import org.bitcoinj.core.Transaction

interface BitcoinChangeListener {

    fun onBalanceChange(value: Long)
    fun onNewTransaction(tx: Transaction)
    fun onTransactionConfidenceChange(tx: Transaction)
    fun onBestChainHeightChange(value: Int)

}
