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
    fun insert(enabledWallet: EnabledWallet): Long

    @Query("DELETE FROM EnabledWallet")
    fun deleteAll()

    @Transaction
    fun insertWallets(enabledWallets: List<EnabledWallet>): List<Long>
        = enabledWallets.map { insert(it) }

    @Query("DELETE FROM EnabledWallet WHERE id IN (:ids)")
    fun deleteWallets(ids: List<Long>)
}
