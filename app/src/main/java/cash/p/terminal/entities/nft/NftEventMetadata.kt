package cash.p.terminal.entities.nft

import cash.p.terminal.wallet.models.NftPrice
import java.util.*

data class NftEventMetadata(
    val assetMetadata: NftAssetMetadata,
    val eventType: EventType?,
    val date: Date?,
    val amount: NftPrice?
) {
    enum class EventType {
        All,
        List,
        Sale,
        OfferEntered,
        BidEntered,
        BidWithdrawn,
        Transfer,
        Approve,
        Custom,
        Payout,
        Cancel,
        BulkCancel,
        Mint,
        Unknown;
    }
}
