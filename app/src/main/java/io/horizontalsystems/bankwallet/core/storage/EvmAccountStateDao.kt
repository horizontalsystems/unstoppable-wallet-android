package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.EvmAccountState

@Dao
interface EvmAccountStateDao {

    @Query("SELECT * FROM EvmAccountState WHERE accountId = :accountId AND chainId = :chainId")
    fun get(accountId: String, chainId: Int): EvmAccountState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(evmAccountState: EvmAccountState)

}
