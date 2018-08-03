package bitcoin.wallet.core.managers

import bitcoin.wallet.core.CollectionChangeset
import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.core.RealmManager
import bitcoin.wallet.entities.Balance
import bitcoin.wallet.entities.BlockchainInfo
import bitcoin.wallet.entities.ExchangeRate
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.bitcoin.BitcoinUnspentOutput
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCashUnspentOutput
import io.reactivex.Observable
import io.realm.Sort
import javax.inject.Inject

class DatabaseManager @Inject constructor(realmManager: RealmManager) : IDatabaseManager {

    private val realm = RealmManager().createWalletRealmLocal()

    override fun getBitcoinUnspentOutputs(): Observable<DatabaseChangeset<BitcoinUnspentOutput>> =
            realm.where(BitcoinUnspentOutput::class.java).findAllAsync()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun getBitcoinCashUnspentOutputs(): Observable<DatabaseChangeset<BitcoinCashUnspentOutput>> =
            realm.where(BitcoinCashUnspentOutput::class.java).findAllAsync()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun getExchangeRates(): Observable<DatabaseChangeset<ExchangeRate>> =
            realm.where(ExchangeRate::class.java).findAllAsync()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun getTransactionRecords(): Observable<DatabaseChangeset<TransactionRecord>> =
            realm.where(TransactionRecord::class.java)
                    .sort("blockHeight", Sort.DESCENDING)
                    .findAllAsync()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun getBalances(): Observable<DatabaseChangeset<Balance>> =
            realm.where(Balance::class.java)
                    .findAllAsync()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun getBlockchainInfos(): Observable<DatabaseChangeset<BlockchainInfo>> =
            realm.where(BlockchainInfo::class.java)
                    .findAllAsync()
                    .asChangesetObservable()
                    .map {
                        DatabaseChangeset(it.collection, it.changeset?.let { CollectionChangeset(it) })
                    }

    override fun insertOrUpdateTransactions(transactionRecords: List<TransactionRecord>) {
        realm.executeTransactionAsync {
            it.insertOrUpdate(transactionRecords)
        }
    }

    override fun updateBlockchainInfo(blockchainInfo: BlockchainInfo) {
        realm.executeTransactionAsync {
            it.insertOrUpdate(blockchainInfo)
        }
    }

    override fun updateBlockchainHeight(coinCode: String, height: Long) {
        var blockchainInfo = realm.where(BlockchainInfo::class.java).equalTo("coinCode", coinCode).findFirst()

        if (blockchainInfo == null) {
            blockchainInfo = BlockchainInfo().apply { this.coinCode = coinCode }
        }

        realm.executeTransaction {
            blockchainInfo.latestBlockHeight = height
            it.insertOrUpdate(blockchainInfo)

        }
    }

    override fun updateBlockchainSyncing(coinCode: String, syncing: Boolean) {
        var blockchainInfo = realm.where(BlockchainInfo::class.java).equalTo("coinCode", coinCode).findFirst()

        if (blockchainInfo == null) {
            blockchainInfo = BlockchainInfo().apply { this.coinCode = coinCode }
        }

        realm.executeTransaction {
            blockchainInfo.syncing = syncing
            it.insertOrUpdate(blockchainInfo)

        }
    }

    override fun updateBalance(balance: Balance) {
        realm.executeTransactionAsync {
            it.insertOrUpdate(balance)
        }
    }

    override fun updateExchangeRate(exchangeRate: ExchangeRate) {
        realm.executeTransactionAsync {
            it.insertOrUpdate(exchangeRate)
        }
    }

    override fun getTransactionRecord(coinCode: String, txHash: String): Observable<TransactionRecord> =
            realm.where(TransactionRecord::class.java)
                    .and().equalTo("coinCode", coinCode).and().equalTo("transactionHash", txHash)
                    .findAllAsync()
                    .asChangesetObservable()
                    .map {
                        it.collection.first()
                    }

    override fun close() {
        realm.close()
    }

}
