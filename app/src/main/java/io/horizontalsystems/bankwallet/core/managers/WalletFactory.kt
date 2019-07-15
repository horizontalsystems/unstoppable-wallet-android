package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWalletFactory
import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode

class WalletFactory : IWalletFactory {

    override fun wallet(coin: Coin, account: Account, syncMode: SyncMode): Wallet {
        return Wallet(coin, account, syncMode)
    }

}
