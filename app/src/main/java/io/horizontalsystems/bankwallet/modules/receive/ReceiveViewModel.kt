package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.accountTypeDerivation
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.TokenType

class ReceiveViewModel(
    val wallet: Wallet,
    receiveAdapter: IReceiveAdapter
) : ViewModel() {

    val receiveAddress  = receiveAdapter.receiveAddress
    val addressType = (wallet.token.type as? TokenType.Derived)?.derivation?.accountTypeDerivation?.addressType
    val watchAccount = wallet.account.isWatchAccount
    val isAccountActive = receiveAdapter.isAccountActive
}
