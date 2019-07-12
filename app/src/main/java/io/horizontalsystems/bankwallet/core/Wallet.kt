package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode

class Wallet(val coin: Coin, val account: Account, val syncMode: SyncMode)
