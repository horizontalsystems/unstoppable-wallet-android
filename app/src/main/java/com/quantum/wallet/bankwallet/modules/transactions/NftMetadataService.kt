package com.quantum.wallet.bankwallet.modules.transactions

import com.quantum.wallet.bankwallet.core.managers.NftMetadataManager
import com.quantum.wallet.bankwallet.entities.nft.NftAssetBriefMetadata
import com.quantum.wallet.bankwallet.entities.nft.NftUid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NftMetadataService(
    val nftMetadataManager: NftMetadataManager
) {
    private val _assetsBriefMetadataFlow = MutableStateFlow<Map<NftUid, NftAssetBriefMetadata>>(mapOf())
    val assetsBriefMetadataFlow = _assetsBriefMetadataFlow.asStateFlow()

    fun assetsBriefMetadata(nftUids: Set<NftUid>): Map<NftUid, NftAssetBriefMetadata> =
        nftMetadataManager.assetsBriefMetadata(nftUids).associateBy { it.nftUid }

    suspend fun fetch(nftUids: Set<NftUid>) {
        //val assetBriefMetadata = nftMetadataManager.fetchAssetsBriefMetadata(nftUids)

        //handle(assetBriefMetadata)
    }

    private fun handle(assetsBriefMetadata: List<NftAssetBriefMetadata>) {
        nftMetadataManager.save(assetsBriefMetadata)

        _assetsBriefMetadataFlow.tryEmit(assetsBriefMetadata.associateBy { it.nftUid })
    }
}