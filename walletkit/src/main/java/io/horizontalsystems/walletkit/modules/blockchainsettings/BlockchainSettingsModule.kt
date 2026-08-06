package io.horizontalsystems.walletkit.modules.blockchainsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.core.managers.ThorchainRpcSource
import io.horizontalsystems.walletkit.core.order
import io.horizontalsystems.walletkit.entities.BtcRestoreMode
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import io.horizontalsystems.marketkit.models.Blockchain

object BlockchainSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service =
                BlockchainSettingsService(
                    App.btcBlockchainManager,
                    App.evmBlockchainManager,
                    App.evmSyncSourceManager,
                    App.thorchainRpcSourceManager,
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

        class Thorchain(
            override val blockchain: Blockchain,
            val rpcSource: ThorchainRpcSource
        ) : BlockchainItem()

        class Chain(
            override val blockchain: Blockchain,
            val subtitle: String,
            val btcLike: Boolean,
            val page: HSPage,
            val statEvent: StatEvent,
        ) : BlockchainItem()

        val order
            get() = blockchain.type.order
    }

}
