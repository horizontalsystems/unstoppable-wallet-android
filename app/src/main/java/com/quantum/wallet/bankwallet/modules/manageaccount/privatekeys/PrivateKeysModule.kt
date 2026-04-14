package com.quantum.wallet.bankwallet.modules.manageaccount.privatekeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.modules.manageaccount.showextendedkey.ShowExtendedKeyModule
import com.quantum.wallet.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyModule.MoneroKeys
import io.horizontalsystems.hdwalletkit.HDExtendedKey

object PrivateKeysModule {

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrivateKeysViewModel(account, App.evmBlockchainManager) as T
        }
    }

    data class ViewState(
        val evmPrivateKey: String? = null,
        val tronPrivateKey: String? = null,
        val bip32RootKey: ExtendedKey? = null,
        val accountExtendedPrivateKey: ExtendedKey? = null,
        val stellarSecretKey: String? = null,
        val moneroKeys: MoneroKeys? = null
    )

    data class ExtendedKey(
        val hdKey: HDExtendedKey,
        val displayKeyType: ShowExtendedKeyModule.DisplayKeyType
    )
}