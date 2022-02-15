package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NftDao {
    @Query("SELECT * FROM NftCollection WHERE accountId = :accountId")
    fun getCollections(accountId: String): Flow<List<NftCollection>>

    @Query("SELECT * FROM NftAsset WHERE accountId = :accountId")
    fun getAssets(accountId: String): Flow<List<NftAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCollections(collections: List<NftCollection>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAssets(assets: List<NftAsset>)

    @Query("DELETE FROM NftCollection WHERE accountId = :accountId")
    fun deleteCollectionsForAccount(accountId: String)

    @Query("DELETE FROM NftAsset WHERE accountId = :accountId")
    fun deleteAssetsForAccount(accountId: String)

    @Transaction
    fun replaceCollectionAssets(accountId: String, collections: List<NftCollection>, assets: List<NftAsset>) {
        deleteCollectionsForAccount(accountId)
        deleteAssetsForAccount(accountId)

        insertCollections(collections)
        insertAssets(assets)
    }
}

