package io.horizontalsystems.bankwallet.modules.market.topnftcollections

import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.modules.nft.TopNftCollection
import java.math.BigDecimal

class TopNftCollectionsViewItemFactory(
    private val numberFormatter: IAppNumberFormatter
) {

    fun viewItem(
        collection: TopNftCollection,
        timeDuration: TimeDuration,
        order: Int
    ): TopNftCollectionViewItem {
        val volume: BigDecimal
        val volumeDiff: BigDecimal
        when (timeDuration) {
            TimeDuration.OneDay -> {
                volume = collection.oneDayVolume
                volumeDiff = collection.oneDayVolumeDiff
            }
            TimeDuration.SevenDay -> {
                volume = collection.sevenDayVolume
                volumeDiff = collection.sevenDayVolumeDiff
            }
            TimeDuration.ThirtyDay -> {
                volume = collection.thirtyDayVolume
                volumeDiff = collection.thirtyDayVolumeDiff
            }
        }
        val volumeFormatted = numberFormatter.formatCoin(volume, "ETH", 0, 2)
        val floorPriceFormatted = collection.floorPrice?.let {
            numberFormatter.format(it, 0, 2, prefix = "Floor: ", suffix = " ETH")
        }

        return TopNftCollectionViewItem(
            uid = collection.uid,
            name = collection.name,
            imageUrl = collection.imageUrl,
            volume = volumeFormatted,
            volumeDiff = volumeDiff * BigDecimal.valueOf(100),
            order = order,
            floorPrice = floorPriceFormatted
        )
    }

}
