package io.horizontalsystems.bankwallet.modules.settings.security.blockchains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*

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
        class Btc(
            val blockchain: BtcBlockchain,
            val restoreMode: BtcRestoreMode,
            val transactionMode: TransactionDataSortMode
        ) : BlockchainItem()

        class Evm(val blockchain: EvmBlockchain, val syncSource: EvmSyncSource) : BlockchainItem()

        val order: Int
            get() = when (this) {
                is Btc -> {
                    when (this.blockchain) {
                        BtcBlockchain.Bitcoin -> 0
                        BtcBlockchain.BitcoinCash -> 100
                        BtcBlockchain.Litecoin -> 101
                        BtcBlockchain.Dash -> 102
                    }
                }
                is Evm -> {
                    when (this.blockchain) {
                        EvmBlockchain.Ethereum -> 2
                        EvmBlockchain.BinanceSmartChain -> 3
                        EvmBlockchain.Polygon -> 4
                        EvmBlockchain.Optimism -> 5
                        EvmBlockchain.ArbitrumOne -> 6
                    }
                }

            }
    }

}
