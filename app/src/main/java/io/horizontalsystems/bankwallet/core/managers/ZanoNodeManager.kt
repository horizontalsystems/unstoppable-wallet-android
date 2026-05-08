package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.ZanoNodeStorage
import io.horizontalsystems.bankwallet.entities.ZanoNodeRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ZanoNodeManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val zanoNodeStorage: ZanoNodeStorage,
    private val marketKitWrapper: MarketKitWrapper,
) {
    private val _currentNodeUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val currentNodeUpdatedFlow = _currentNodeUpdatedFlow.asSharedFlow()

    private val _nodesUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodesUpdatedFlow = _nodesUpdatedFlow.asSharedFlow()

    val defaultNodesInitial = listOf(
        ZanoNode("https://zano.unstoppable.money:443", "zano.unstoppable.money"),
        ZanoNode("https://node.zano.org:443", "node.zano.org"),
        ZanoNode("http://37.27.100.59:10500", "37.27.100.59"),
    )

    val defaultNodes: List<ZanoNode> get() = defaultNodesInitial

    val customNodes: List<ZanoNode>
        get() {
            val defaultUrls = defaultNodesInitial.map { it.host }
            return zanoNodeStorage.getAll()
                .filterNot { defaultUrls.contains(it.url) }
                .map { ZanoNode(it.url, it.url) }
        }

    val allNodes: List<ZanoNode>
        get() = defaultNodes + customNodes

    val currentNode: ZanoNode
        get() {
            val host = blockchainSettingsStorage.zanoNodeHost()
            return allNodes.firstOrNull { it.host == host } ?: defaultNodes.first()
        }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(BlockchainType.Zano.uid)

    fun save(node: ZanoNode) {
        blockchainSettingsStorage.saveZanoNode(node.host)
        _currentNodeUpdatedFlow.tryEmit(node.host)
    }

    fun addZanoNode(url: String) {
        zanoNodeStorage.save(ZanoNodeRecord(url))
        customNodes.firstOrNull { it.host == url }?.let { save(it) }
        _nodesUpdatedFlow.tryEmit(url)
    }

    fun delete(node: ZanoNode) {
        val isCurrent = node == currentNode
        zanoNodeStorage.delete(node.host)
        if (isCurrent) {
            save(defaultNodes.first())
        }
        _nodesUpdatedFlow.tryEmit(node.host)
    }

    data class ZanoNode(val host: String, val name: String)
}
