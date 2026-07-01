package io.horizontalsystems.bankwallet.core.adapters.nft

import io.horizontalsystems.bankwallet.entities.nft.NftRecord
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger


interface INftAdapter {
    val userAddress: String
    val nftRecordsFlow: Flow<List<NftRecord>>
    val nftRecords: List<NftRecord>
    fun nftRecord(nftUid: NftUid): NftRecord?
    fun sync()
    fun transferEip721TransactionData(
        contractAddress: String,
        to: Address,
        tokenId: String
    ): TransactionData?

    fun transferEip1155TransactionData(
        contractAddress: String,
        to: Address,
        tokenId: String,
        value: BigInteger
    ): TransactionData?
}