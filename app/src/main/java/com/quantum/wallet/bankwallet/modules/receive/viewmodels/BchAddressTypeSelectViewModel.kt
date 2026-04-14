package com.quantum.wallet.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.managers.WalletManager
import com.quantum.wallet.bankwallet.core.bitcoinCashCoinType
import com.quantum.wallet.bankwallet.modules.receive.ui.AddressFormatItem
import io.horizontalsystems.marketkit.models.TokenType

class BchAddressTypeSelectViewModel(coinUid: String, walletManager: WalletManager) : ViewModel() {
    val items = walletManager.activeWallets
        .filter {
            it.coin.uid == coinUid
        }
        .mapNotNull { wallet ->
            val addressType =
                (wallet.token.type as? TokenType.AddressTyped)?.type ?: return@mapNotNull null
            val bitcoinCashCoinType = addressType.bitcoinCashCoinType

            AddressFormatItem(
                title = bitcoinCashCoinType.title,
                subtitle = bitcoinCashCoinType.value.uppercase(),
                wallet = wallet
            )
        }

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BchAddressTypeSelectViewModel(coinUid, App.walletManager) as T
        }
    }
}

