package cash.p.terminal.modules.swapxxx
>>>>>>>> b890ba0e9 (Restructure classes):app/src/main/java/cash.p.terminal/modules/swapxxx/EvmBlockchainHelper.kt

import cash.p.terminal.core.App
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.marketkit.models.BlockchainType

class EvmBlockchainHelper(private val blockchainType: BlockchainType) {
    val evmKitWrapper = App.evmBlockchainManager
        .getEvmKitManager(blockchainType)
        .evmKitWrapper

    private val evmKit = evmKitWrapper?.evmKit

    val baseToken by lazy { App.evmBlockchainManager.getBaseToken(blockchainType) }
    val receiveAddress by lazy { evmKit?.receiveAddress }
    val chain by lazy { App.evmBlockchainManager.getChain(blockchainType) }

    fun getRpcSourceHttp(): RpcSource.Http {
        val httpSyncSource = App.evmSyncSourceManager.getHttpSyncSource(blockchainType)
        return httpSyncSource?.rpcSource as? RpcSource.Http
            ?: throw IllegalStateException("No HTTP RPC Source for blockchain $blockchainType")
    }

}
