package io.horizontalsystems.walletkit.modules.manageaccount.privatekeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.core.chain.ChainKeyRow

object PrivateKeysModule {

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrivateKeysViewModel(account, App.evmBlockchainManager) as T
        }
    }

    data class ViewState(
        val tronPrivateKey: String? = null,
        val chainKeyRows: List<ChainKeyRow> = emptyList()
    )
}