package io.horizontalsystems.bankwallet.modules.profeatures.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProFeaturesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sessionKeyRow: ProFeaturesSessionKey)

    @Query("SELECT * FROM ProFeaturesSessionKey WHERE nftName = :nftName LIMIT 1")
    fun getOne(nftName: String): ProFeaturesSessionKey?

    @Query("DELETE FROM ProFeaturesSessionKey WHERE accountId NOT IN (:accountIds)")
    fun deleteAllExcept(accountIds: List<String>)

    @Query("DELETE FROM ProFeaturesSessionKey")
    fun clear()

}
