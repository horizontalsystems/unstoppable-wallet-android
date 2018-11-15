package io.horizontalsystems.bankwallet.modules.wallet

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue

class WalletViewItem(
        var coinValue: CoinValue,
        var exchangeValue: CurrencyValue?,
        var currencyValue: CurrencyValue?,
        var state: AdapterState,
        var rateExpired: Boolean
)
