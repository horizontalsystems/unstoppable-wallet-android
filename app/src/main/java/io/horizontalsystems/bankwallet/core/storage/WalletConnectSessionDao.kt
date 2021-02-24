package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.entities.WalletConnectSession

@Dao
interface WalletConnectSessionDao {

    @Query("SELECT * FROM WalletConnectSession")
    fun getAll(): List<WalletConnectSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(session: WalletConnectSession)

    @Query("DELETE FROM WalletConnectSession")
    fun deleteAll()

}
