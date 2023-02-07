package cash.p.terminal.modules.evmprivatekey

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.Account

object EvmPrivateKeyModule {
    const val ACCOUNT = "account"

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EvmPrivateKeyViewModel(account, App.evmBlockchainManager) as T
        }
    }

    fun prepareParams(account: Account) = bundleOf(ACCOUNT to account)

}
