package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.OcpPaymentRecord

@Dao
interface OcpPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: OcpPaymentRecord)

    @Query("SELECT * FROM OcpPaymentRecord WHERE txHash = :txHash LIMIT 1")
    suspend fun getByTxHash(txHash: String): OcpPaymentRecord?

    @Query("SELECT * FROM OcpPaymentRecord WHERE proofSubmittedAt IS NULL AND proofFailedAt IS NULL")
    suspend fun getPending(): List<OcpPaymentRecord>

    @Query("UPDATE OcpPaymentRecord SET proofSubmittedAt = :submittedAt WHERE txHash = :txHash")
    suspend fun markSubmitted(txHash: String, submittedAt: Long)

    @Query("UPDATE OcpPaymentRecord SET proofFailedAt = :failedAt WHERE txHash = :txHash")
    suspend fun markFailed(txHash: String, failedAt: Long)
}
