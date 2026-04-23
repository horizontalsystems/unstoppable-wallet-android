package cash.p.terminal.modules.syncerror

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.SolanaKitManager
import cash.p.terminal.wallet.IAdapterManager
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.entities.Blockchain

object SyncErrorModule {

    class Factory(private val wallet: cash.p.terminal.wallet.Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = SyncErrorService(
                wallet,
                getKoinInstance<IAdapterManager>(),
                AppConfigProvider.reportEmail,
                getKoinInstance<BtcBlockchainManager>(),
                getKoinInstance<EvmBlockchainManager>(),
                getKoinInstance<SolanaKitManager>(),
                getKoinInstance<ISystemInfoManager>()
            )
            return SyncErrorViewModel(service) as T
        }
    }

    data class BlockchainWrapper(
        val blockchain: Blockchain,
        val type: Type
    ) {
        enum class Type {
            Bitcoin, Evm
        }
    }
}
