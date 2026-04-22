package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cash.p.terminal.entities.PoisonAddress

@Dao
interface PoisonAddressDao {
    @Query("SELECT * FROM PoisonAddress WHERE address = :address AND blockchainTypeUid = :blockchainTypeUid AND accountId = :accountId")
    fun get(address: String, blockchainTypeUid: String, accountId: String): PoisonAddress?

    @Query("""
        SELECT * FROM PoisonAddress
        WHERE type = 'KNOWN'
          AND blockchainTypeUid = :blockchainTypeUid
          AND accountId = :accountId
          AND sendCount >= :minSendCount
    """)
    fun getWhitelisted(
        blockchainTypeUid: String,
        accountId: String,
        minSendCount: Int,
    ): List<PoisonAddress>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(poisonAddress: PoisonAddress)

    @Query("""
        UPDATE PoisonAddress
        SET sendCount = sendCount + 1
        WHERE address = :address
          AND blockchainTypeUid = :blockchainTypeUid
          AND accountId = :accountId
          AND type = 'KNOWN'
    """)
    fun incrementKnownSendCount(
        address: String,
        blockchainTypeUid: String,
        accountId: String,
    ): Int

    @Query("""
        INSERT OR REPLACE INTO PoisonAddress(address, blockchainTypeUid, accountId, type, sendCount)
        VALUES (:address, :blockchainTypeUid, :accountId, 'KNOWN', 1)
    """)
    fun insertFirstKnown(
        address: String,
        blockchainTypeUid: String,
        accountId: String,
    )

    @Transaction
    fun upsertKnownIncrementingCount(
        address: String,
        blockchainTypeUid: String,
        accountId: String,
    ) {
        if (incrementKnownSendCount(address, blockchainTypeUid, accountId) == 0) {
            insertFirstKnown(address, blockchainTypeUid, accountId)
        }
    }
}
