package cash.p.terminal.entities.nft

import cash.p.terminal.wallet.models.NftPrice
import java.math.BigDecimal

data class NftPriceRecord(
    val tokenQueryId: String,
    val value: BigDecimal
) {
    constructor(nftPrice: NftPrice) : this(nftPrice.token.tokenQuery.id, nftPrice.value)
}