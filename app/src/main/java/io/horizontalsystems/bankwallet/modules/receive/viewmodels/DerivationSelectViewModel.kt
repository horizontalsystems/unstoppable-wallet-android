package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.accountTypeDerivation
import io.horizontalsystems.bankwallet.modules.receive.ui.AddressFormatItem
import io.horizontalsystems.marketkit.models.TokenType

class DerivationSelectViewModel(coinUid: String, walletManager: IWalletManager) : ViewModel() {
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