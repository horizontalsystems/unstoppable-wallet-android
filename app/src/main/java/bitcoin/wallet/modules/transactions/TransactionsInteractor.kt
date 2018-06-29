package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.managers.CoinManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.TransactionRecord
import java.util.*

class TransactionsInteractor(private val databaseManager: IDatabaseManager, private val coinManager: CoinManager) : TransactionsModule.IInteractor {

    var delegate: TransactionsModule.IInteractorDelegate? = null

    override fun retrieveTransactionRecords() {

        databaseManager.getTransactionRecords().subscribe({ transactions ->
            delegate?.didRetrieveTransactionRecords(transactions.array.mapNotNull { transactionRecord ->
                convert(transactionRecord)
            })
        })

    }

    private fun convert(transactionRecord: TransactionRecord): TransactionRecordViewItem? {
        val coin = coinManager.getCoinByCode(transactionRecord.coinCode)

        return if (coin == null) {
            null
        } else {
            TransactionRecordViewItem(
                    transactionRecord.transactionHash,
                    CoinValue(coin, transactionRecord.amount / 100000000.0),
                    CoinValue(coin, transactionRecord.fee / 100000000.0),
                    transactionRecord.from,
                    transactionRecord.to,
                    transactionRecord.incoming,
                    transactionRecord.blockHeight,
                    Date(transactionRecord.timestamp),
                    null,
                    null

            )
        }
    }

}
