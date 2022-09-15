package io.horizontalsystems.bankwallet.core.storage

import android.util.Log
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.nft.*
import io.horizontalsystems.marketkit.models.NftPrice
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

class NftStorage(
    private val nftDao: NftDao,
    private val marketKit: MarketKitWrapper
) {
    fun addressInfo(nftKey: NftKey): NftAddressMetadata? = try {
        val collectionRecords = nftDao.getCollections(nftKey.blockchainType, nftKey.account.id)
        val assetRecords = nftDao.getAssets(nftKey.blockchainType, nftKey.account.id)
        val priceRecords =
            collectionRecords.mapNotNull { it.averagePrice7d } + collectionRecords.mapNotNull { it.averagePrice30d } + assetRecords.mapNotNull { it.lastSale }

        val tokens = marketKit.tokens(tokenQueries(priceRecords))
        val collections = collectionRecords.map {
            NftCollectionShortMetadata(
                providerUid = it.uid,
                name = it.name,
                thumbnailImageUrl = it.imageUrl,
                averagePrice7d = nftPrice(it.averagePrice7d, tokens),
                averagePrice30 = nftPrice(it.averagePrice30d, tokens)
            )
        }
        val assets = assetRecords.map {
            NftAssetShortMetadata(
                nftUid = it.nftUid,
                providerCollectionUid = it.collectionUid,
                name = it.name,
                previewImageUrl = it.imagePreviewUrl,
                onSale = it.onSale,
                lastSalePrice = nftPrice(it.lastSale, tokens)
            )
        }
        NftAddressMetadata(collections, assets)
    } catch (exception: Exception) {
        null
    }

    fun save(addressMetadata: NftAddressMetadata, nftKey: NftKey) = try {
        val collectionRecords = addressMetadata.collections.map {
            NftCollectionRecord(
                blockchainType = nftKey.blockchainType,
                accountId = nftKey.account.id,
                uid = it.providerUid,
                name = it.name,
                imageUrl = it.thumbnailImageUrl,
                averagePrice7d = it.averagePrice7d?.let { _ -> NftPriceRecord(it.averagePrice7d) },
                averagePrice30d = it.averagePrice30?.let { _ -> NftPriceRecord(it.averagePrice30) }
            )
        }
        val assetRecords = addressMetadata.assets.map {
            NftAssetRecord(
                blockchainType = nftKey.blockchainType,
                accountId = nftKey.account.id,
                nftUid = it.nftUid,
                collectionUid = it.providerCollectionUid,
                name = it.name,
                imagePreviewUrl = it.previewImageUrl,
                onSale = it.onSale,
                lastSale = it.lastSalePrice?.let { _ -> NftPriceRecord(it.lastSalePrice) }
            )
        }

        nftDao.replaceCollectionAssets(
            blockchainType = nftKey.blockchainType,
            accountId = nftKey.account.id,
            collectionRecords = collectionRecords,
            assetRecords = assetRecords
        )
    } catch (exception: Exception) {
        Log.e("AAA", "Could not save NftAddressInfo")
    }

    fun lastSyncTimestamp(nftKey: NftKey): Long? = try {
        nftDao.getNftMetadataSyncRecord(nftKey.blockchainType, nftKey.account.id)?.lastSyncTimestamp
    } catch (exception: Exception) {
        Log.e("AAA", "Could not fetch NftMetadataSyncRecord: ${exception.message ?: exception.javaClass.simpleName}")
        null
    }

    fun save(lastSyncTimestamp: Long, nftKey: NftKey) = try {
        val record = NftMetadataSyncRecord(nftKey.blockchainType, nftKey.account.id, lastSyncTimestamp)
        nftDao.insertNftMetadataSyncRecord(record)
    } catch (exception: Exception) {
        Log.e("AAA", "Could not save NftMetadataSyncRecord: ${exception.message ?: exception.javaClass.simpleName}")
    }

    private fun tokenQueries(priceRecords: List<NftPriceRecord>): List<TokenQuery> =
        priceRecords.map { it.tokenQueryId }.distinct().mapNotNull { TokenQuery.fromId(it) }

    private fun nftPrice(priceRecord: NftPriceRecord?, tokens: List<Token>): NftPrice? =
        priceRecord?.let {
            tokens.firstOrNull { it.tokenQuery.id == priceRecord.tokenQueryId }?.let { token ->
                NftPrice(token, priceRecord.value)
            }
        }
}