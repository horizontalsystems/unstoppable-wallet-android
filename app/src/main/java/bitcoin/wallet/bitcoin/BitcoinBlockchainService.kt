package bitcoin.wallet.bitcoin

import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.blockchain.IBlockchainService
import bitcoin.wallet.entities.Balance
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.log
import io.reactivex.subjects.PublishSubject
import org.bitcoinj.core.listeners.DownloadProgressTracker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BitcoinBlockchainService @Inject constructor(private val storage: BlockchainStorage, private val bitcoinJWrapper: BitcoinJWrapper) : IBlockchainService {

    private var updateTransactionsSubject = PublishSubject.create<Map<String, TransactionRecord>>()

    private val BTC = "BTC"

    private var txs = ConcurrentHashMap<String, TransactionRecord>()

    override fun initNewWallet() {
        updateBalance(0)
        updateBlockHeight(0)
    }

    override fun start(paperKeys: List<String>) {
        updateTransactionsSubject.sample(2, TimeUnit.SECONDS).subscribe {
            dequeueTransactionUpdate()
        }

        bitcoinJWrapper.prepareEnvForWallet(paperKeys, object : BitcoinChangeListener {
            override fun onBalanceChange(value: Long) {
                updateBalance(value)
            }

            override fun onNewTransaction(tx: TransactionRecord) {
                enqueueTransactionUpdate(tx)
            }

            override fun onTransactionConfidenceChange(tx: TransactionRecord) {
                enqueueTransactionUpdate(tx)
            }

            override fun onBestChainHeightChange(value: Int) {
                updateBlockHeight(value.toLong())
            }
        })

        bitcoinJWrapper.startAsync(object : DownloadProgressTracker() {
            override fun startDownload(blocks: Int) {
                super.startDownload(blocks)
                storage.updateBlockchainSyncing(BTC, true)
            }

            override fun doneDownload() {
                super.doneDownload()
                storage.updateBlockchainSyncing(BTC, false)
            }
        })
    }

    override fun getReceiveAddress(): String = bitcoinJWrapper.getReceiveAddress()

    override fun sendCoins(address: String, value: Long) = bitcoinJWrapper.sendCoins(address, value)

    private fun updateBalance(balance: Long) {
        storage.updateBalance(Balance().apply {
            code = BTC
            value = balance
        })
    }

    private fun updateBlockHeight(height: Long) {
        storage.updateBlockchainHeight(BTC, height)
    }

    private fun enqueueTransactionUpdate(tx: TransactionRecord) {
        synchronized(txs) {
            txs[tx.transactionHash] = tx
            txs.size.log("Transactions count: ")

            updateTransactionsSubject.onNext(txs)
        }
    }

    private fun dequeueTransactionUpdate() {
        synchronized(txs) {
            val transactionRecords = mutableListOf<TransactionRecord>()
            txs.forEach {
                val tx = it.value

                // collect items for bulk write/update
                transactionRecords.add(tx)

                // remove item from queue
                txs.remove(tx.transactionHash)
            }

            storage.insertOrUpdateTransactions(transactionRecords)
        }
    }

}
