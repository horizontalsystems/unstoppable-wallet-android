package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionStats
import io.horizontalsystems.marketkit.models.CoinType

class NftItemFactory(private val coinManager: ICoinManager) {

    private val platformCoinEth by lazy { coinManager.getPlatformCoin(CoinType.Ethereum) }

    fun createNftCollectionItem(
        collectionRecord: NftCollectionRecord,
        assetItems: List<NftAssetItem>
    ) = NftCollectionItem(
        slug = collectionRecord.slug,
        name = collectionRecord.name,
        imageUrl = collectionRecord.imageUrl,
        assets = assetItems
    )

    fun createNftAssetItem(
        assetRecord: NftAssetRecord,
        collectionStats: NftCollectionStats?,
        priceType: PriceType
    ): NftAssetItem {
        var averagePrice7d: CoinValue? = null
        var averagePrice30d: CoinValue? = null

        collectionStats?.let { stats ->
            platformCoinEth?.let {
                averagePrice7d = CoinValue(
                    CoinValue.Kind.PlatformCoin(it),
                    stats.averagePrice7d
                )
                averagePrice30d = CoinValue(
                    CoinValue.Kind.PlatformCoin(it),
                    stats.averagePrice30d
                )
            }
        }

        val lastPrice: CoinValue? = assetRecord.lastSale?.let { lastSale ->
            coinManager.getPlatformCoin(CoinType.fromId(lastSale.coinTypeId))
                ?.let { platformCoin ->
                    CoinValue(
                        CoinValue.Kind.PlatformCoin(platformCoin),
                        lastSale.totalPrice
                    )
                }
        }

        val assetItemPrices = NftAssetItem.Prices(
            average7d = averagePrice7d,
            average30d = averagePrice30d,
            last = lastPrice
        )

        val coinPrice = when (priceType) {
            PriceType.Days7 -> assetItemPrices.average7d
            PriceType.Days30 -> assetItemPrices.average30d
            PriceType.LastPrice -> assetItemPrices.last
        }

        return NftAssetItem(
            accountId = assetRecord.accountId,
            tokenId = assetRecord.tokenId,
            name = assetRecord.name,
            imageUrl = assetRecord.imageUrl,
            imagePreviewUrl = assetRecord.imagePreviewUrl,
            ownedCount = assetRecord.ownedCount,
            coinPrice = coinPrice,
            currencyPrice = null,
            onSale = true,
            prices = assetItemPrices
        )
    }

    fun createNftAssetItem(
        assetRecord: NftAssetRecord,
        collectionStats: NftCollectionStats?
    ): NftAssetItem {
        var averagePrice7d: CoinValue? = null
        var averagePrice30d: CoinValue? = null

        collectionStats?.let { stats ->
            platformCoinEth?.let {
                averagePrice7d = CoinValue(
                    CoinValue.Kind.PlatformCoin(it),
                    stats.averagePrice7d
                )
                averagePrice30d = CoinValue(
                    CoinValue.Kind.PlatformCoin(it),
                    stats.averagePrice30d
                )
            }
        }

        val lastPrice: CoinValue? = assetRecord.lastSale?.let { lastSale ->
            coinManager.getPlatformCoin(CoinType.fromId(lastSale.coinTypeId))
                ?.let { platformCoin ->
                    CoinValue(
                        CoinValue.Kind.PlatformCoin(platformCoin),
                        lastSale.totalPrice
                    )
                }
        }

        val assetItemPrices = NftAssetItem.Prices(
            average7d = averagePrice7d,
            average30d = averagePrice30d,
            last = lastPrice
        )

        return NftAssetItem(
            accountId = assetRecord.accountId,
            tokenId = assetRecord.tokenId,
            name = assetRecord.name,
            imageUrl = assetRecord.imageUrl,
            imagePreviewUrl = assetRecord.imagePreviewUrl,
            ownedCount = assetRecord.ownedCount,
            coinPrice = null,
            currencyPrice = null,
            onSale = true,
            prices = assetItemPrices
        )
    }

}