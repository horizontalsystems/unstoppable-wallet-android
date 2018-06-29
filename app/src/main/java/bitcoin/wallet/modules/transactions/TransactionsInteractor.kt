package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.CollectionChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.managers.CoinManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem.Status.PENDING
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem.Status.SUCCESS
import java.util.*
import kotlin.math.max

class TransactionsInteractor(private val databaseManager: IDatabaseManager, private val coinManager: CoinManager) : TransactionsModule.IInteractor {

    var delegate: TransactionsModule.IInteractorDelegate? = null

    private var latestBlockHeights = mapOf<String, Long>()
    private var transactionRecords = listOf<TransactionRecord>()

    override fun retrieveTransactionRecords() {

        databaseManager.getBlockchainInfos().subscribe {
            latestBlockHeights = it.array.map { it.coinCode to it.latestBlockHeight }.toMap()

            refresh()
        }

        databaseManager.getTransactionRecords().subscribe { transactions ->
            transactionRecords = transactions.array

            refresh(transactions.changeset)
        }

    }

    private fun refresh(changeset: CollectionChangeset? = null) {

        delegate?.didRetrieveTransactionRecords(transactionRecords.mapNotNull { transactionRecord ->
            convert(transactionRecord)
        })

    }

    private fun convert(transactionRecord: TransactionRecord): TransactionRecordViewItem? {
        val coin = coinManager.getCoinByCode(transactionRecord.coinCode)
        val latestBlockHeight = latestBlockHeights[transactionRecord.coinCode]

        if (coin == null || latestBlockHeight == null) {
            return null
        }

        val confirmations =
                if (transactionRecord.blockHeight == 0L) {
                    0
                } else {
                    max(0, latestBlockHeight - transactionRecord.blockHeight + 1)
                }

        return TransactionRecordViewItem(
                transactionRecord.transactionHash,
                CoinValue(coin, transactionRecord.amount / 100000000.0),
                CoinValue(coin, transactionRecord.fee / 100000000.0),
                transactionRecord.from,
                transactionRecord.to,
                transactionRecord.incoming,
                transactionRecord.blockHeight,
                Date(transactionRecord.timestamp),
                if (confirmations > 0) SUCCESS else PENDING,
                confirmations

        )

    }

}
