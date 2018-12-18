package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class Wallet(val coinCode: CoinCode, val title: String, val adapter: IAdapter)
