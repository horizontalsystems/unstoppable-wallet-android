package cash.p.terminal.core.storage

import androidx.room.*
import cash.p.terminal.wallet.entities.EnabledWallet

@Dao
interface EnabledWalletsDao {

    @Query("SELECT * FROM EnabledWallet ORDER BY `walletOrder` ASC")
    fun enabledCoins(): List<EnabledWallet>

    @Query("SELECT * FROM EnabledWallet WHERE accountId = :accountId ORDER BY `walletOrder` ASC")
    fun enabledCoins(accountId: String): List<EnabledWallet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(enabledWallet: EnabledWallet)

    @Query("DELETE FROM EnabledWallet")
    fun deleteAll()

    @Transaction
    fun insertWallets(enabledWallets: List<EnabledWallet>) {
        enabledWallets.forEach { insert(it) }
    }

    @Query("DELETE FROM EnabledWallet WHERE LOWER(tokenQueryId) IN (:tokenQueryIds) AND LOWER(accountId) IN (:accountIds)")
    fun deleteWallets(tokenQueryIds: List<String>, accountIds: List<String>)
}
