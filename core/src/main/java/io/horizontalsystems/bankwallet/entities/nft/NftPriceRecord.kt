package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.NftPrice
import java.math.BigDecimal

data class NftPriceRecord(
    val tokenQueryId: String,
    val value: BigDecimal
) {
    constructor(nftPrice: NftPrice) : this(nftPrice.token.tokenQuery.id, nftPrice.value)
}