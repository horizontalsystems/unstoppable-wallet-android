package io.horizontalsystems.solanakit.database.transaction.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.solanakit.models.MintAccount

@Dao
interface MintAccountDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(mintAccount: MintAccount)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(accounts: List<MintAccount>)

    @Query("SELECT * FROM `MintAccount` WHERE address = :address LIMIT 1")
    suspend fun get(address: String): MintAccount?

}
