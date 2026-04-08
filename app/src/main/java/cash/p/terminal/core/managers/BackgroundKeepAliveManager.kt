package cash.p.terminal.core.managers

import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackgroundKeepAliveManager {

    private val _keepAliveBlockchains = MutableStateFlow<Set<BlockchainType>>(emptySet())
    val keepAliveBlockchains = _keepAliveBlockchains.asStateFlow()

    fun setKeepAlive(blockchainTypes: Set<BlockchainType>) {
        _keepAliveBlockchains.value = blockchainTypes
    }

    fun clear() {
        _keepAliveBlockchains.value = emptySet()
    }

    fun isKeepAlive(blockchainType: BlockchainType): Boolean {
        return blockchainType in _keepAliveBlockchains.value
    }
}
