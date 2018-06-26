package bitcoin.wallet.core.managers

import bitcoin.wallet.core.CollectionChangeset
import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.RealmFactory
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.UnspentOutput
import io.reactivex.Observable
import io.realm.Sort

class DatabaseManager : IDatabaseManager {

    private val realm = RealmFactory.createWalletRealm()

    override fun getUnspentOutputs(): Observable<DatabaseChangeset<UnspentOutput>> =
            realm.where(UnspentOutput::class.java).findAll()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun getExchangeRates(): Observable<DatabaseChangeset<ExchangeRate>> =
            realm.where(ExchangeRate::class.java).findAll()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun getTransactionRecords(): Observable<DatabaseChangeset<TransactionRecord>> =
            realm.where(TransactionRecord::class.java)
                    .sort("blockHeight", Sort.DESCENDING)
                    .findAll()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    fun close() {
        realm.close()
    }

}
