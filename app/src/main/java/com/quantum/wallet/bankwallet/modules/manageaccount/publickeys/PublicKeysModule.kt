package com.quantum.wallet.bankwallet.modules.manageaccount.publickeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule.DisplayKeyType.AccountPublicKey
import com.quantum.wallet.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyModule.MoneroKeys
import io.horizontalsystems.hdwalletkit.HDExtendedKey

object PublicKeysModule {

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PublicKeysViewModel(account, App.evmBlockchainManager) as T
        }
    }

    data class ViewState(
        val evmAddress: String? = null,
        val tronAddress: String? = null,
        val extendedPublicKey: ExtendedPublicKey? = null,
        val moneroKeys: MoneroKeys? = null
    )

    data class ExtendedPublicKey(
        val hdKey: HDExtendedKey,
        val accountPublicKey: AccountPublicKey
    )
}