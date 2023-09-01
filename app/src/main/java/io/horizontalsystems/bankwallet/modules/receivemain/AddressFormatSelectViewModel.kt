package io.horizontalsystems.bankwallet.modules.receivemain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IWalletManager

class AddressFormatSelectViewModel(coinUid: String, walletManager: IWalletManager) : ViewModel() {
    val coinWallets = walletManager.activeWallets.filter { it.coin.uid == coinUid }

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddressFormatSelectViewModel(coinUid, App.walletManager) as T
        }
    }
}
