package io.horizontalsystems.bankwallet.modules.walletconnect.request

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue

sealed class WalletConnectRequestViewItem {

    class From(val value: String) : WalletConnectRequestViewItem()
    class To(val value: String) : WalletConnectRequestViewItem()
    class Fee(val coinValue: CoinValue, val currencyValue: CurrencyValue?) : WalletConnectRequestViewItem()

}
