package cash.p.terminal.modules.manageaccount.privatekeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.wallet.Account
import cash.p.terminal.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import io.horizontalsystems.hdwalletkit.HDExtendedKey

object PrivateKeysModule {

    class Factory(private val account: cash.p.terminal.wallet.Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrivateKeysViewModel(account, App.evmBlockchainManager) as T
        }
    }

    data class ViewState(
        val evmPrivateKey: String? = null,
        val bip32RootKey: ExtendedKey? = null,
        val accountExtendedPrivateKey: ExtendedKey? = null,
    )

    data class ExtendedKey(
        val hdKey: HDExtendedKey,
        val displayKeyType: ShowExtendedKeyModule.DisplayKeyType
    )
}