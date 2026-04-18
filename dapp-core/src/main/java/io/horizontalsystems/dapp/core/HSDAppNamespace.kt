package io.horizontalsystems.dapp.core

data class HSDAppNamespaceSession(
    val chains: List<String>?,
    val methods: List<String>,
    val events: List<String>,
    val accounts: List<String>,
)

data class HSDAppNamespaceProposal(
    val chains: List<String>?,
    val methods: List<String>,
    val events: List<String>,
)
