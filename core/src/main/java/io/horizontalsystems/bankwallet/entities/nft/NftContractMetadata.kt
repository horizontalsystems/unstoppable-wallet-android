package io.horizontalsystems.bankwallet.entities.nft

data class NftContractMetadata(
    val address: String,
    val name: String,
    val createdDate: String,
    val schemaName: String?
)
