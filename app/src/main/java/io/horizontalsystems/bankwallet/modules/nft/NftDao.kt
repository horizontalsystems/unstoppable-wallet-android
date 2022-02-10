package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NftDao {

    @Query("SELECT * FROM NftCollection WHERE accountId = :accountId")
    fun getCollections(accountId: String): Flow<List<NftCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCollections(collections: List<NftCollection>)

    @Query("DELETE FROM NftCollection WHERE accountId = :accountId")
    fun deleteCollectionsForAccount(accountId: String)

    @Transaction
    fun replaceCollections(accountId: String, collections: List<NftCollection>) {
        deleteCollectionsForAccount(accountId)
        insertCollections(collections)
    }
}

