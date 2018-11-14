package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.modules.transactions.Coin

class Wallet(val coin: Coin, val adapter: IAdapter)
