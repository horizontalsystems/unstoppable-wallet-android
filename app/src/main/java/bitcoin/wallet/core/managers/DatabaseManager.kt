package bitcoin.wallet.core.managers

import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.RealmFactory
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.UnspentOutput
import io.reactivex.Observable

class DatabaseManager : IDatabaseManager {

    private val realm = RealmFactory.createWalletRealm()

    override fun getUnspentOutputs(): Observable<DatabaseChangeset<UnspentOutput>> =
            realm.where(UnspentOutput::class.java).findAll()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset)
                    }

    override fun getExchangeRates(): Observable<DatabaseChangeset<ExchangeRate>> =
            realm.where(ExchangeRate::class.java).findAll()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset)
                    }

    fun close() {
        realm.close()
    }

}
