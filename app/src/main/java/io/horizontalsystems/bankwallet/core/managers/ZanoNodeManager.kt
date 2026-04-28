package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ZanoNodeManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
) {
    private val _currentNodeUpdatedFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val currentNodeUpdatedFlow = _currentNodeUpdatedFlow.asSharedFlow()

    val defaultNodes = listOf(
        ZanoNode("https://zano.unstoppable.money:443", "zano.unstoppable.money"),
        ZanoNode("zano.miner.rocks:11211", "zano.miner.rocks"),
    )

    val currentNode: ZanoNode
        get() {
            val host = blockchainSettingsStorage.zanoNodeHost()
            return defaultNodes.firstOrNull { it.host == host } ?: defaultNodes.first()
        }

    fun save(node: ZanoNode) {
        blockchainSettingsStorage.saveZanoNode(node.host)
        _currentNodeUpdatedFlow.tryEmit(node.host)
    }

    data class ZanoNode(val host: String, val name: String)
}
