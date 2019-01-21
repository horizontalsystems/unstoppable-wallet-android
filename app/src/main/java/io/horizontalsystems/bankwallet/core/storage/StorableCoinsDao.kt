package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.horizontalsystems.bankwallet.entities.StorableCoin
import io.reactivex.Flowable

@Dao
interface StorableCoinsDao {

    @Query("SELECT * FROM StorableCoin WHERE coinCode = :coinCode")
    fun getCoin(coinCode: String): Flowable<StorableCoin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(storableCoin: StorableCoin)

    @Query("SELECT * FROM StorableCoin")
    fun getAll(): Flowable<List<StorableCoin>>

    @Query("DELETE FROM StorableCoin")
    fun deleteAll()

}
