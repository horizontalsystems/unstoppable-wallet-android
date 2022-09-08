package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.market.overview.coinValue
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.NftCollection
import io.horizontalsystems.marketkit.models.NftPrice
import java.math.BigDecimal
import java.math.RoundingMode

data class NftCollectionItem(
    val uid: String,
    val name: String,
    val imageUrl: String?,
    val floorPrice: CoinValue?,
    val totalVolume: BigDecimal?,
    val oneDayVolume: CoinValue?,
    val oneDayVolumeDiff: BigDecimal?,
    val sevenDayVolume: CoinValue?,
    val sevenDayVolumeDiff: BigDecimal?,
    val thirtyDayVolume: CoinValue?,
    val thirtyDayVolumeDiff: BigDecimal?
)

val NftCollection.nftCollectionItem: NftCollectionItem
    get() = NftCollectionItem(
        uid = uid,
        name = name,
        imageUrl = imageUrl,
        floorPrice = stats.floorPrice?.coinValue,
        totalVolume = stats.totalVolume,
        oneDayVolume = stats.volumes[HsTimePeriod.Day1]?.coinValue,
        oneDayVolumeDiff = stats.changes[HsTimePeriod.Day1],
        sevenDayVolume = stats.volumes[HsTimePeriod.Week1]?.coinValue,
        sevenDayVolumeDiff = stats.changes[HsTimePeriod.Week1],
        thirtyDayVolume = stats.volumes[HsTimePeriod.Month1]?.coinValue,
        thirtyDayVolumeDiff = stats.changes[HsTimePeriod.Month1]
    )

val NftCollection.oneDayVolumeChange: BigDecimal?
    get() {
        val firstValue = statCharts?.oneDayVolumePoints?.firstOrNull()?.value
        val lastValue = statCharts?.oneDayVolumePoints?.lastOrNull()?.value

        return diff(firstValue, lastValue)
    }

val NftCollection.oneDayAveragePriceChange: BigDecimal?
    get() {
        val firstValue = statCharts?.averagePricePoints?.firstOrNull()?.value
        val lastValue = statCharts?.averagePricePoints?.lastOrNull()?.value

        return diff(firstValue, lastValue)
    }

val NftCollection.oneDaySalesChange: BigDecimal?
    get() {
        val firstValue = statCharts?.oneDaySalesPoints?.firstOrNull()?.value
        val lastValue = statCharts?.oneDaySalesPoints?.lastOrNull()?.value

        return diff(firstValue, lastValue)
    }

val NftCollection.oneDayFloorPriceChange: BigDecimal?
    get() {
        val firstValue = statCharts?.floorPricePoints?.firstOrNull()?.value
        val lastValue = statCharts?.floorPricePoints?.lastOrNull()?.value

        return diff(firstValue, lastValue)
    }

val NftPrice.nftPriceRecord: NftPriceRecord
    get() = NftPriceRecord(token.tokenQuery.id, value)

private fun diff(firstValue: BigDecimal?, lastValue: BigDecimal?) =
    if (firstValue != null && lastValue != null) {
        lastValue.subtract(firstValue).divide(firstValue, 4, RoundingMode.HALF_UP)
    } else {
        null
    }
