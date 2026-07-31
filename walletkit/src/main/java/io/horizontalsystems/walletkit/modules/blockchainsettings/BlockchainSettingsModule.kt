package io.horizontalsystems.walletkit.modules.blockchainsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.MoneroNodeManager.MoneroNode
import io.horizontalsystems.walletkit.core.managers.ZanoNodeManager.ZanoNode
import io.horizontalsystems.walletkit.core.managers.ThorchainRpcSource
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.ZcashEndpoint
import io.horizontalsystems.walletkit.core.order
import io.horizontalsystems.walletkit.entities.BtcRestoreMode
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.solanakit.models.RpcSource

object BlockchainSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service =
                BlockchainSettingsService(
                    App.btcBlockchainManager,
                    App.evmBlockchainManager,
                    App.evmSyncSourceManager,
                    App.solanaRpcSourceManager,
                    App.thorchainRpcSourceManager,
                    App.moneroNodeManager,
                    App.zanoNodeManager,
                    App.zcashEndpointManager,
                    App.marketKit
                )
            return BlockchainSettingsViewModel(service) as T
        }
    }

    data class BlockchainViewItem(
        val title: String,
        val subtitle: String,
        val imageUrl: String,
        val blockchainItem: BlockchainItem
    )

    sealed class BlockchainItem {
        abstract val blockchain: Blockchain

        class Btc(
            override val blockchain: Blockchain,
            val restoreMode: BtcRestoreMode
        ) : BlockchainItem()

        class Evm(
            override val blockchain: Blockchain,
            val syncSource: EvmSyncSource
        ) : BlockchainItem()

        class Solana(
            override val blockchain: Blockchain,
            val rpcSource: RpcSource
        ) : BlockchainItem()

        class Thorchain(
            override val blockchain: Blockchain,
            val rpcSource: ThorchainRpcSource
        ) : BlockchainItem()

        class Monero(
            override val blockchain: Blockchain,
            val node: MoneroNode
        ) : BlockchainItem()

        class Zano(
            override val blockchain: Blockchain,
            val node: ZanoNode
        ) : BlockchainItem()

        class Zcash(
            override val blockchain: Blockchain,
            val endpoint: ZcashEndpoint
        ) : BlockchainItem()

        val order
            get() = blockchain.type.order
    }

}
