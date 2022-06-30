package io.horizontalsystems.bankwallet.modules.settings.security.blockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.EvmSyncSource
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.marketkit.models.Blockchain

object BlockchainSettingsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service =
                BlockchainSettingsService(
                    App.btcBlockchainManager,
                    App.evmBlockchainManager,
                    App.evmSyncSourceManager
                )
            return BlockchainSettingsViewModel(service) as T
        }
    }

    data class BlockchainViewItem(
        val title: String,
        val subtitle: String,
        val icon: Int,
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

        val order
            get() = blockchain.type.order
    }

}
