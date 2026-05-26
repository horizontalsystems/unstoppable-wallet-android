package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.bitcoinCashCoinType
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.modules.receive.ui.AddressFormatItem
import io.horizontalsystems.marketkit.models.TokenType

@HiltViewModel(assistedFactory = BchAddressTypeSelectViewModel.Factory::class)
class BchAddressTypeSelectViewModel @AssistedInject constructor(
    @Assisted coinUid: String,
    walletManager: WalletManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(coinUid: String): BchAddressTypeSelectViewModel
    }

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
}

