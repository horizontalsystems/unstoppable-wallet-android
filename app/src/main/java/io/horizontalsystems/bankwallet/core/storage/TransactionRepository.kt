package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.ITransactionRecordStorage
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.Maybe
import java.util.concurrent.Executors

class TransactionRepository(private val appDatabase: AppDatabase) : ITransactionRecordStorage {

    private val executor = Executors.newSingleThreadExecutor()

    override fun record(hash: String): Maybe<TransactionRecord> =
            appDatabase.transactionDao().getByHash(hash)

    override val nonFilledRecords: Maybe<List<TransactionRecord>>
        get() = appDatabase.transactionDao().getNonFilledRecord()

    override fun set(rate: Double, transactionHash: String) {
        appDatabase.transactionDao().updateRate(rate, transactionHash)
    }

    override fun clearRates() {
        executor.execute {
            appDatabase.transactionDao().clearRates()
        }
    }

    override fun update(records: List<TransactionRecord>) {
        executor.execute {
            appDatabase.transactionDao().insertAll(records)
        }
    }

    override fun clearRecords() {
        executor.execute {
            appDatabase.transactionDao().deleteAll()
        }
    }

}
