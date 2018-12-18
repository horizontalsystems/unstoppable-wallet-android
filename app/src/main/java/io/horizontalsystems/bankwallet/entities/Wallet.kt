package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

class Wallet(val title: String, val coinCode: CoinCode, val adapter: IAdapter)
