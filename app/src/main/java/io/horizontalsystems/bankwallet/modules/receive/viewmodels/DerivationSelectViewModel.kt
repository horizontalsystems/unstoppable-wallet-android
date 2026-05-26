package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.accountTypeDerivation
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.modules.receive.ui.AddressFormatItem
import io.horizontalsystems.marketkit.models.TokenType

@HiltViewModel(assistedFactory = DerivationSelectViewModel.Factory::class)
class DerivationSelectViewModel @AssistedInject constructor(
    @Assisted coinUid: String,
    walletManager: WalletManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(coinUid: String): DerivationSelectViewModel
    }

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
}