package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.PoisonAddress
import cash.p.terminal.entities.PoisonAddressType

@Dao
interface PoisonAddressDao {
    @Query("SELECT * FROM PoisonAddress WHERE address = :address AND blockchainTypeUid = :blockchainTypeUid AND accountId = :accountId")
    fun get(address: String, blockchainTypeUid: String, accountId: String): PoisonAddress?

    @Query("SELECT * FROM PoisonAddress WHERE type = :type AND blockchainTypeUid = :blockchainTypeUid AND accountId = :accountId")
    fun getAllByType(type: PoisonAddressType, blockchainTypeUid: String, accountId: String): List<PoisonAddress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(poisonAddress: PoisonAddress)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(poisonAddress: PoisonAddress)
}
