package io.horizontalsystems.bankwallet.modules.manageaccount.privatekeys

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import io.horizontalsystems.hdwalletkit.HDExtendedKey

object PrivateKeysModule {

    const val ACCOUNT_KEY = "account_key"
    fun prepareParams(account: Account) = bundleOf(ACCOUNT_KEY to account)

    class Factory(private val account: Account) : ViewModelProvider.Factory {
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