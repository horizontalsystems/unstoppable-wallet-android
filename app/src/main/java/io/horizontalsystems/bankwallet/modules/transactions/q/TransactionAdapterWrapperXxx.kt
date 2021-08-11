package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionWallet
import io.reactivex.Single

class TransactionAdapterWrapperXxx(
    private val transactionsAdapter: ITransactionsAdapter,
    private val transactionWallet: TransactionWallet
) {

    private var from: TransactionRecord? = null

    fun getNext(limit: Int): Single<List<TransactionRecord>> {
        return transactionsAdapter.getTransactionsAsync(from, transactionWallet.coin, limit)
    }

    fun markUsed(record: TransactionRecord?) {
        from = record
    }

}
