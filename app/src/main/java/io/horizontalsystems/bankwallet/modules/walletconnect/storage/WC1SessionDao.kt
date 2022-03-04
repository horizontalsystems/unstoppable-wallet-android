package io.horizontalsystems.bankwallet.modules.walletconnect.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.horizontalsystems.bankwallet.modules.walletconnect.entity.WalletConnectSession

@Dao
interface WC1SessionDao {

    @Query("SELECT * FROM WalletConnectSession WHERE accountId = :accountId AND chainId IN(:chainIds)")
    fun getByAccountId(accountId: String, chainIds: List<Int>): List<WalletConnectSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(session: WalletConnectSession)

    @Query("DELETE FROM WalletConnectSession WHERE accountId NOT IN (:accountIds)")
    fun deleteAllExcept(accountIds: List<String>)

    @Query("DELETE FROM WalletConnectSession WHERE remotePeerId = :peerId")
    fun deleteByPeerId(peerId: String)

}
