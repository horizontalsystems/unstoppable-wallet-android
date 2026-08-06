package io.horizontalsystems.walletkit.core.managers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.solanakit.models.RpcSource

class SolanaRpcSourceManager(
        private val blockchainSettingsStorage: BlockchainSettingsStorage,
        private val marketKitWrapper: MarketKitWrapper,
        alchemyApiKey: String,
) {

    private val blockchainType = BlockchainType.Solana
    private val _rpcSourceUpdateFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val rpcSourceUpdateFlow: SharedFlow<Unit>
        get() = _rpcSourceUpdateFlow.asSharedFlow()

    val allRpcSources = listOf(RpcSource.Alchemy(alchemyApiKey))

    val rpcSource: RpcSource
        get() {
            val rpcSourceName = blockchainSettingsStorage.evmSyncSourceUrl(blockchainType)
            val rpcSource = allRpcSources.firstOrNull { it.name == rpcSourceName }

            return rpcSource ?: allRpcSources[0]
        }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(blockchainType.uid)

    fun save(rpcSource: RpcSource) {
        blockchainSettingsStorage.save(rpcSource.name, blockchainType)
        _rpcSourceUpdateFlow.tryEmit(Unit)
    }

}
