package io.horizontalsystems.bankwallet.modules.walletconnect.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WCSessionDao {

    @Query("SELECT * FROM WalletConnectV2Session")
    fun getAll(): List<WalletConnectV2Session>

    @Query("SELECT * FROM WalletConnectV2Session WHERE accountId = :accountId")
    fun getByAccountId(accountId: String): List<WalletConnectV2Session>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sessions: List<WalletConnectV2Session>)

    @Query("DELETE FROM WalletConnectV2Session WHERE topic IN (:topics)")
    fun deleteByTopics(topics: List<String>)

    @Query("DELETE FROM WalletConnectV2Session WHERE accountId NOT IN (:accountIds)")
    fun deleteAllExcept(accountIds: List<String>)

}
