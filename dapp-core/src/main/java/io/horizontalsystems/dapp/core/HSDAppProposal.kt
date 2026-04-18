package io.horizontalsystems.dapp.core

data class HSDAppProposal(
    val proposerPublicKey: String,
    val name: String,
    val url: String,
    val description: String,
    val icons: List<String>,
    val requiredNamespaces: Map<String, HSDAppNamespaceProposal>,
    val optionalNamespaces: Map<String, HSDAppNamespaceProposal>,
)
