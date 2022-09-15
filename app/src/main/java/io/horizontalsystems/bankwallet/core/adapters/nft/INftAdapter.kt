package io.horizontalsystems.bankwallet.core.adapters.nft

import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import kotlinx.coroutines.flow.Flow


interface INftAdapter {
    val userAddress: String
    val nftRecordsFlow: Flow<List<NftRecord>>
    val nftRecords: List<NftRecord>
    fun sync()
}