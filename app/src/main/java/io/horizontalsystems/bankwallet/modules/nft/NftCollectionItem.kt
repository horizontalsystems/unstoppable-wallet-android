package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.market.overview.coinValue
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.NftCollection
import java.math.BigDecimal

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