package io.horizontalsystems.bankwallet.core.storage

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface AccountsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(accountRow: AccountRecord)

    @Update
    fun update(accountRow: AccountRecord)

    @Query("UPDATE AccountRecord SET deleted = 1 WHERE id = :id")
    fun delete(id: String)

    @Query("SELECT * FROM AccountRecord WHERE deleted = 0")
    fun getAll(): List<AccountRecord>

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
}
