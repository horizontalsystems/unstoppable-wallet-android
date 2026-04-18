package io.horizontalsystems.dapp.core

data class HSDAppRequest(
    val topic: String,
    val chainId: String?,
    val method: String,
    val requestId: Long,
    val params: String,
    val peerMetaData: HSDAppAppMetaData?,
)
