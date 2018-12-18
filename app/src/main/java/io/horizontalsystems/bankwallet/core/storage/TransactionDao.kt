package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
interface TransactionDao {

    @Query("SELECT * FROM TransactionRecord WHERE transactionHash = :txHash")
    fun getByHash(txHash: String): Maybe<TransactionRecord>

    @Query("SELECT * FROM TransactionRecord WHERE rate = 0")
    fun getNonFilledRecord(): Maybe<List<TransactionRecord>>

    @Query("UPDATE TransactionRecord SET rate = :rate WHERE transactionHash = :txHash")
    fun updateRate(rate: Double, txHash: String)

    @Query("UPDATE TransactionRecord SET rate = 0")
    fun clearRates()

    @Query("SELECT * FROM TransactionRecord ORDER BY timestamp DESC")
    fun getAll(): Flowable<List<TransactionRecord>>

    @Query("SELECT * FROM TransactionRecord where coinCode = :coinCode ORDER BY timestamp DESC")
    fun getAll(coinCode: CoinCode): Flowable<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(transactionRecords: List<TransactionRecord>)

    @Query("DELETE FROM TransactionRecord")
    fun deleteAll()

}
