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

    @Query("DELETE FROM WalletConnectSession WHERE accountId NOT IN (:accountIds)")
    fun deleteAllExcept(accountIds: List<String>)

    @Query("DELETE FROM WalletConnectSession WHERE remotePeerId = :peerId")
    fun deleteByPeerId(peerId: String)

}
