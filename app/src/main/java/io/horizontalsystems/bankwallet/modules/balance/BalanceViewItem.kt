package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue

class BalanceViewItem(
        var coinValue: CoinValue,
        var exchangeValue: CurrencyValue?,
        var currencyValue: CurrencyValue?,
        var state: AdapterState,
        var rateExpired: Boolean
)
