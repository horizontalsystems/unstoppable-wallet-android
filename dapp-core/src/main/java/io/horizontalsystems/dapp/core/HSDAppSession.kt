package io.horizontalsystems.dapp.core

data class HSDAppSession(
    val topic: String,
    val metaData: HSDAppAppMetaData?,
    val namespaces: Map<String, HSDAppNamespaceSession>,
)
