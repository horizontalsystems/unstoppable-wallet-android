package io.horizontalsystems.bankwallet.modules.manageaccount.publickeys

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey
import io.horizontalsystems.hdwalletkit.HDExtendedKey

object PublicKeysModule {

    const val ACCOUNT_KEY = "account_key"
    fun prepareParams(account: Account) = bundleOf(ACCOUNT_KEY to account)

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PublicKeysViewModel(account, App.evmBlockchainManager) as T
        }
    }

    data class ViewState(
        val evmAddress: String? = null,
        val extendedPublicKey: ExtendedPublicKey? = null
    )

    data class ExtendedPublicKey(
        val hdKey: HDExtendedKey,
        val accountPublicKey: AccountPublicKey
    )
}