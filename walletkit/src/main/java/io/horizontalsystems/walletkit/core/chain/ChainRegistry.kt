package io.horizontalsystems.walletkit.core.chain

import io.horizontalsystems.marketkit.models.BlockchainType

/**
 * Registry of [ChainPlugin]s, populated once during app initialization. Registration order
 * is preserved and irrelevant to display order (which comes from `BlockchainType.order`).
 */
object ChainRegistry {

    private val plugins = LinkedHashMap<BlockchainType, ChainPlugin>()

    fun register(plugin: ChainPlugin) {
        plugins[plugin.blockchainType] = plugin
    }

    operator fun get(blockchainType: BlockchainType): ChainPlugin? = plugins[blockchainType]

    val all: Collection<ChainPlugin>
        get() = plugins.values
}
