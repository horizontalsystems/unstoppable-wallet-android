package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.Bitcoin
import bitcoin.wallet.entities.CoinValue
import java.util.*

class TransactionsInteractor(private val databaseManager: IDatabaseManager) : TransactionsModule.IInteractor {

    var delegate: TransactionsModule.IInteractorDelegate? = null

    override fun retrieveTransactionRecords() {

        databaseManager.getTransactionRecords().subscribe({ transactions ->
            delegate?.didRetrieveTransactionRecords(transactions.array.map {
                TransactionRecordViewItem(
                        it.hash,
                        CoinValue(Bitcoin(), it.amount / 100000000.0),
                        CoinValue(Bitcoin(), it.fee / 100000000.0),
                        it.from,
                        it.to,
                        it.incoming,
                        it.blockHeight,
                        Date(it.timestamp),
                        null,
                        null

                )
            })
        })

    }

}
