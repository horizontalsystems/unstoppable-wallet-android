package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionStats
import io.horizontalsystems.marketkit.models.CoinType

class NftItemFactory(private val coinManager: ICoinManager) {

    private val platformCoinEth by lazy { coinManager.getPlatformCoin(CoinType.Ethereum) }

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
            description = assetRecord.description,
            ownedCount = assetRecord.ownedCount,
            contract = assetRecord.contract,
            onSale = true,
            prices = assetItemPrices
        )
    }

}