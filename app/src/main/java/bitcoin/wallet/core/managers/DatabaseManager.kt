package bitcoin.wallet.core.managers

import bitcoin.wallet.core.CollectionChangeset
import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.RealmFactory
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.bitcoin.BitcoinUnspentOutput
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCashUnspentOutput
import io.reactivex.Observable
import io.realm.Sort

class DatabaseManager : IDatabaseManager {

    private val realm = RealmFactory.createWalletRealm()

    override fun getBitcoinUnspentOutputs(): Observable<DatabaseChangeset<BitcoinUnspentOutput>> =
            realm.where(BitcoinUnspentOutput::class.java).findAll()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun getBitcoinCashUnspentOutputs(): Observable<DatabaseChangeset<BitcoinCashUnspentOutput>> =
            realm.where(BitcoinCashUnspentOutput::class.java).findAll()
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
