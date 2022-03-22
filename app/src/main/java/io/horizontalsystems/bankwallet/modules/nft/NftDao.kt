package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NftDao {
    @Query("SELECT * FROM NftCollectionRecord WHERE accountId = :accountId")
    suspend fun getCollections(accountId: String): List<NftCollectionRecord>

    @Query("SELECT * FROM NftAssetRecord WHERE accountId = :accountId")
    suspend fun getAssets(accountId: String): List<NftAssetRecord>

    @Query("SELECT * FROM NftAssetRecord WHERE accountId = :accountId AND tokenId = :tokenId AND contract_address = :contractAddress")
    suspend fun getAsset(accountId: String, tokenId: String, contractAddress: String): NftAssetRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCollections(collectionRecords: List<NftCollectionRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAssets(assetRecords: List<NftAssetRecord>)

    @Query("DELETE FROM NftCollectionRecord WHERE accountId = :accountId")
    fun deleteCollectionsForAccount(accountId: String)

    @Query("DELETE FROM NftAssetRecord WHERE accountId = :accountId")
    fun deleteAssetsForAccount(accountId: String)

    @Transaction
    fun replaceCollectionAssets(accountId: String, collectionRecords: List<NftCollectionRecord>, assetRecords: List<NftAssetRecord>) {
        deleteCollectionsForAccount(accountId)
        deleteAssetsForAccount(accountId)

        insertCollections(collectionRecords)
        insertAssets(assetRecords)
    }

    @Query("SELECT * FROM NftCollectionRecord WHERE accountId = :accountId AND uid = :slug")
    fun getCollection(accountId: String, slug: String): NftCollectionRecord?
}

