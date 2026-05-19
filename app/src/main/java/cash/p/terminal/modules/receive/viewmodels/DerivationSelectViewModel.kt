package cash.p.terminal.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.description
import cash.p.terminal.core.title
import cash.p.terminal.modules.receive.ui.AddressFormatItem
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.accountTypeDerivation
import cash.p.terminal.wallet.entities.TokenType

class DerivationSelectViewModel(coinUid: String, walletManager: IWalletManager) : ViewModel() {
    val items = walletManager.activeWallets
        .filter {
            it.coin.uid == coinUid
        }
        .mapNotNull { wallet ->
            when (val type = wallet.token.type) {
                is TokenType.Derived -> {
                    val accountTypeDerivation = type.derivation.accountTypeDerivation
                    AddressFormatItem(
                        title = accountTypeDerivation.addressType + accountTypeDerivation.recommended,
                        subtitle = accountTypeDerivation.value.uppercase(),
                        wallet = wallet
                    )
                }

                TokenType.Mweb -> {
                    AddressFormatItem(
                        title = type.description,
                        subtitle = type.title,
                        wallet = wallet
                    )
                }

                else -> null
            }
        }

    class Factory(private val coinUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DerivationSelectViewModel(coinUid, App.walletManager) as T
        }
    }
}
