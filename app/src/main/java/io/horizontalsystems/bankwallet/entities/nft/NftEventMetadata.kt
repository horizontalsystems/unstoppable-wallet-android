package io.horizontalsystems.bankwallet.entities.nft

import io.horizontalsystems.marketkit.models.NftEvent
import io.horizontalsystems.marketkit.models.NftPrice
import java.util.*

data class NftEventMetadata(
    val assetMetadata: NftAssetMetadata,
    val eventType: NftEvent.EventType,
    val date: Date?,
    val amount: NftPrice?
)