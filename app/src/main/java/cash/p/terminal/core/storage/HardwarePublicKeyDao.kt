package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cash.p.terminal.wallet.entities.HardwarePublicKey
import cash.p.terminal.wallet.entities.TokenType

@Dao
interface HardwarePublicKeyDao {
    @Query("SELECT * FROM HardwarePublicKey WHERE accountId = :accountId")
    fun getAllPublicKeys(accountId: String): List<HardwarePublicKey>

    @Query("SELECT * FROM HardwarePublicKey WHERE accountId = :accountId AND blockchainType = :blockchainType AND tokenType = :tokenType")
    suspend fun getPublicKey(accountId: String, blockchainType: String, tokenType: TokenType): HardwarePublicKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(key: HardwarePublicKey): Long

    @Query("DELETE FROM HardwarePublicKey")
    fun deleteAll()

    @Transaction
    suspend fun insertKeys(keys: List<HardwarePublicKey>): List<Long> = keys.map { insert(it) }

    @Query("DELETE FROM EnabledWallet WHERE id IN (:ids)")
    fun deleteKeys(ids: List<Long>)
}
