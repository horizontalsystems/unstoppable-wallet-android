package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.App
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.core.entities.BlockchainType

class EvmBlockchainHelper(private val blockchainType: BlockchainType) {
    val chain by lazy { App.evmBlockchainManager.getChain(blockchainType) }

    fun getRpcSourceHttp(): RpcSource.Http {
        val httpSyncSource = App.evmSyncSourceManager.getHttpSyncSource(blockchainType)
        return httpSyncSource?.rpcSource as? RpcSource.Http
            ?: throw IllegalStateException("No HTTP RPC Source for blockchain $blockchainType")
    }

}
