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

    @Query("DELETE FROM AccountRecord WHERE id=:id")
    fun delete(id: String)

    @Query("SELECT * FROM AccountRecord")
    fun getAll(): List<AccountRecord>

    @Query("SELECT COUNT(*) FROM AccountRecord WHERE isBackedUp = 0")
    fun getNonBackedUpCount(): Flowable<Int>

}
