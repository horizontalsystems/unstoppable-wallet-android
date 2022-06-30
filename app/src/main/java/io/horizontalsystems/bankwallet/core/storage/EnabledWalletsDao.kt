package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.EnabledWallet

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

    @Query("DELETE FROM EnabledWallet WHERE tokenQueryId = :tokenQueryId AND accountId = :accountId AND coinSettingsId = :coinSettingsId")
    fun delete(tokenQueryId: String, accountId: String, coinSettingsId: String)

    @Transaction
    fun insertWallets(enabledWallets: List<EnabledWallet>) {
        enabledWallets.forEach { insert(it) }
    }

    @Transaction
    fun deleteWallets(enabledWallets: List<EnabledWallet>) {
        enabledWallets.forEach { delete(it.tokenQueryId, it.accountId, it.coinSettingsId) }
    }

}
