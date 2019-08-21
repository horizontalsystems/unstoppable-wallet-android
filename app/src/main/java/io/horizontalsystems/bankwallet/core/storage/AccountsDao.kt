package io.horizontalsystems.bankwallet.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Flowable

@Dao
interface AccountsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(accountRow: AccountRecord)

    @Query("UPDATE AccountRecord SET deleted = 1 WHERE id = :id")
    fun delete(id: String)

    @Query("SELECT * FROM AccountRecord WHERE deleted = 0")
    fun getAll(): List<AccountRecord>

    @Query("SELECT * FROM AccountRecord WHERE deleted = 1")
    fun getDeleted(): List<AccountRecord>

    @Query("SELECT COUNT(*) FROM AccountRecord WHERE isBackedUp = 0 AND deleted = 0")
    fun getNonBackedUpCount(): Flowable<Int>

    @Query("SELECT COUNT(*) FROM AccountRecord WHERE deleted = 0")
    fun getTotalCount(): Int

    @Query("UPDATE AccountRecord SET deleted = 1")
    fun deleteAll()

    @Query("DELETE FROM AccountRecord WHERE deleted = 1")
    fun clearDeleted()
}
