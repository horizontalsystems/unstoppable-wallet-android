package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.horizontalsystems.bankwallet.entities.ActiveAccount
import io.reactivex.Flowable

@Dao
interface AccountsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(accountRow: AccountRecord)

    @Update
    fun update(accountRow: AccountRecord)

    @Query("UPDATE AccountRecord SET deleted = 1 WHERE id = :id")
    fun delete(id: String)

    @Query("UPDATE AccountRecord SET level = :level WHERE id IN(:accountIds)")
    fun updateLevels(accountIds: List<String>, level: Int)

    @Query("UPDATE AccountRecord SET level = :level WHERE level > :level")
    fun updateMaxLevel(level: Int)

    @Query("SELECT * FROM AccountRecord WHERE deleted = 0 AND level >= :accountsMinLevel")
    fun getAll(accountsMinLevel: Int): List<AccountRecord>

    @Query("SELECT id FROM AccountRecord WHERE deleted = 1")
    fun getDeletedIds(): List<String>

    @Query("SELECT COUNT(*) FROM AccountRecord WHERE isBackedUp = 0 AND deleted = 0")
    fun getNonBackedUpCount(): Flowable<Int>

    @Query("SELECT COUNT(*) FROM AccountRecord WHERE deleted = 0")
    fun getTotalCount(): Int

    @Query("UPDATE AccountRecord SET deleted = 1")
    fun deleteAll()

    @Query("DELETE FROM AccountRecord WHERE deleted = 1")
    fun clearDeleted()

    @Query("SELECT * FROM ActiveAccount LIMIT 1")
    fun getActiveAccount(): ActiveAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertActiveAccount(activeAccount: ActiveAccount)

    @Query("DELETE FROM ActiveAccount")
    fun deleteActiveAccount()
}
