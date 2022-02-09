package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NftDao {

    @Query("SELECT * FROM NftCollection WHERE accountId = :accountId")
    fun getCollections(accountId: String): Flow<List<NftCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCollections(collections: List<NftCollection>)
}

