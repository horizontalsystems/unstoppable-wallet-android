package cash.p.terminal.modules.receive

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.IReceiveAdapter
import cash.p.terminal.core.accountTypeDerivation
import cash.p.terminal.entities.Wallet
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
