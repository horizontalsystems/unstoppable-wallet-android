package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.ScannedTransaction
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.marketkit.models.BlockchainType

@Dao
interface ScannedTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(scannedTransaction: ScannedTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(scannedTransactions: List<ScannedTransaction>)

    @Query("SELECT * FROM ScannedTransaction WHERE transactionHash = :hash LIMIT 1")
    fun getByHash(hash: ByteArray): ScannedTransaction?

    @Query("SELECT * FROM ScannedTransaction WHERE transactionHash IN (:hashes)")
    fun getByHashes(hashes: List<ByteArray>): List<ScannedTransaction>

    @Query("SELECT * FROM ScannedTransaction WHERE spamScore >= 7 AND address = :address LIMIT 1")
    fun getSpamByAddress(address: String): ScannedTransaction?

    @Query("SELECT * FROM ScannedTransaction WHERE spamScore >= 7")
    fun getAllSpam(): List<ScannedTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(spamScanState: SpamScanState)

    @Query("SELECT * FROM SpamScanState WHERE blockchainType = :blockchainType AND accountId = :accountId")
    fun getSpamScanState(blockchainType: BlockchainType, accountId: String): SpamScanState?
}