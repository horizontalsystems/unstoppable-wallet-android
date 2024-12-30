package cash.p.terminal.wallet.models

import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.models.HsTimePeriod
import java.math.BigDecimal

class NftTopCollection(
    val blockchainType: BlockchainType,
    val providerUid: String,
    val name: String,
    val thumbnailImageUrl: String?,
    val floorPrice: NftPrice?,
    val volumes: Map<HsTimePeriod, NftPrice>,
    val changes: Map<HsTimePeriod, BigDecimal>
)
