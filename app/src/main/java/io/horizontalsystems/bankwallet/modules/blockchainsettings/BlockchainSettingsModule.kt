package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
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
                    App.solanaRpcSourceManager
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
            val restoreMode: BtcRestoreMode,
            val transactionMode: TransactionDataSortMode
        ) : BlockchainItem()

        class Evm(
            override val blockchain: Blockchain,
            val syncSource: EvmSyncSource
        ) : BlockchainItem()

        class Solana(
            override val blockchain: Blockchain,
            val rpcSource: RpcSource
        ) : BlockchainItem()

        val order
            get() = blockchain.type.order
    }

}
