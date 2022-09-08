package io.horizontalsystems.bankwallet.modules.nft

import androidx.room.*
import io.horizontalsystems.marketkit.models.BlockchainType

@Dao
interface NftDao {
    @Query("SELECT * FROM NftCollectionRecord WHERE blockchainType = :blockchainType AND accountId = :accountId")
    fun getCollections(blockchainType: BlockchainType, accountId: String): List<NftCollectionRecord>

    @Query("SELECT * FROM NftAssetRecord WHERE  blockchainType = :blockchainType AND accountId = :accountId")
    fun getAssets(blockchainType: BlockchainType, accountId: String): List<NftAssetRecord>

//    @Query("SELECT * FROM NftAssetRecord WHERE accountId = :accountId AND tokenId = :tokenId AND contract_address = :contractAddress")
//    suspend fun getAsset(accountId: String, tokenId: String, contractAddress: String): NftAssetRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCollections(collectionRecords: List<NftCollectionRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAssets(assetRecords: List<NftAssetRecord>)

    @Query("DELETE FROM NftCollectionRecord WHERE blockchainType = :blockchainType AND accountId = :accountId ")
    fun deleteCollectionsForAccount(blockchainType: BlockchainType, accountId: String)

    @Query("DELETE FROM NftAssetRecord WHERE blockchainType = :blockchainType AND accountId = :accountId")
    fun deleteAssetsForAccount(blockchainType: BlockchainType, accountId: String)

    @Transaction
    fun replaceCollectionAssets(
        blockchainType: BlockchainType,
        accountId: String,
        collectionRecords: List<NftCollectionRecord>,
        assetRecords: List<NftAssetRecord>
    ) {
        deleteCollectionsForAccount(blockchainType, accountId)
        deleteAssetsForAccount(blockchainType, accountId)

        insertCollections(collectionRecords)
        insertAssets(assetRecords)
    }

    @Query("SELECT * FROM NftCollectionRecord WHERE accountId = :accountId AND uid = :slug")
    fun getCollection(accountId: String, slug: String): NftCollectionRecord?

    @Query("SELECT * FROM NftMetadataSyncRecord WHERE blockchainType = :blockchainType AND accountId = :accountId")
    fun getNftMetadataSyncRecord(blockchainType: BlockchainType, accountId: String): NftMetadataSyncRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNftMetadataSyncRecord(syncRecord: NftMetadataSyncRecord)
}

