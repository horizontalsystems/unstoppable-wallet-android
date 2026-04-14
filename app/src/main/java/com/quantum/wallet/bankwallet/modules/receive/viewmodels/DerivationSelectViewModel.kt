package com.quantum.wallet.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.managers.WalletManager
import com.quantum.wallet.bankwallet.core.accountTypeDerivation
import com.quantum.wallet.bankwallet.modules.receive.ui.AddressFormatItem
import io.horizontalsystems.marketkit.models.TokenType

class DerivationSelectViewModel(coinUid: String, walletManager: WalletManager) : ViewModel() {
    val items = walletManager.activeWallets
        .filter {
            it.coin.uid == coinUid
        }
        .mapNotNull { wallet ->
            val derivation =
                (wallet.token.type as? TokenType.Derived)?.derivation ?: return@mapNotNull null
            val accountTypeDerivation = derivation.accountTypeDerivation

            AddressFormatItem(
                title = accountTypeDerivation.addressType + accountTypeDerivation.recommended,
                subtitle = accountTypeDerivation.value.uppercase(),
                wallet = wallet
            )
        }

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DerivationSelectViewModel(coinUid, App.walletManager) as T
        }
    }
}