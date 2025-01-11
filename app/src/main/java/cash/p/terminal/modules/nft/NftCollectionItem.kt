package cash.p.terminal.modules.nft

import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.market.overview.coinValue
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.models.HsTimePeriod
import cash.p.terminal.wallet.models.NftTopCollection
import java.math.BigDecimal

data class NftCollectionItem(
    val blockchainType: BlockchainType,
    val uid: String,
    val name: String,
    val imageUrl: String?,
    val floorPrice: CoinValue?,
    val oneDayVolume: CoinValue?,
    val oneDayVolumeDiff: BigDecimal?,
    val sevenDayVolume: CoinValue?,
    val sevenDayVolumeDiff: BigDecimal?,
    val thirtyDayVolume: CoinValue?,
    val thirtyDayVolumeDiff: BigDecimal?
)

val NftTopCollection.nftCollectionItem: NftCollectionItem
    get() = NftCollectionItem(
        blockchainType = blockchainType,
        uid = providerUid,
        name = name,
        imageUrl = thumbnailImageUrl,
        floorPrice = floorPrice?.coinValue,
        oneDayVolume = volumes[HsTimePeriod.Day1]?.coinValue,
        oneDayVolumeDiff = changes[HsTimePeriod.Day1],
        sevenDayVolume = volumes[HsTimePeriod.Week1]?.coinValue,
        sevenDayVolumeDiff = changes[HsTimePeriod.Week1],
        thirtyDayVolume = volumes[HsTimePeriod.Month1]?.coinValue,
        thirtyDayVolumeDiff = changes[HsTimePeriod.Month1]
    )