package cash.p.terminal.core.storage

import androidx.room.*
import cash.p.terminal.wallet.entities.EnabledWallet

@Dao
interface EnabledWalletsDao {

    @Query("SELECT * FROM EnabledWallet ORDER BY `walletOrder` ASC")
    fun enabledCoins(): List<cash.p.terminal.wallet.entities.EnabledWallet>

    @Query("SELECT * FROM EnabledWallet WHERE accountId = :accountId ORDER BY `walletOrder` ASC")
    fun enabledCoins(accountId: String): List<cash.p.terminal.wallet.entities.EnabledWallet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(enabledWallet: cash.p.terminal.wallet.entities.EnabledWallet)

    @Query("DELETE FROM EnabledWallet")
    fun deleteAll()

    @Transaction
    fun insertWallets(enabledWallets: List<cash.p.terminal.wallet.entities.EnabledWallet>) {
        enabledWallets.forEach { insert(it) }
    }

    @Delete
    fun deleteWallets(enabledWallets: List<cash.p.terminal.wallet.entities.EnabledWallet>)

}
