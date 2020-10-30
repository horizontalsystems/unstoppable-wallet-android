package io.horizontalsystems.bankwallet.modules.walletconnect.request

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue

sealed class WalletConnectRequestViewItem {

    class To(val value: String) : WalletConnectRequestViewItem()
    class Input(val value: String) : WalletConnectRequestViewItem()

}
