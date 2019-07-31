package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IWalletFactory
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.horizontalsystems.bankwallet.entities.Wallet

class WalletFactory : IWalletFactory {

    override fun wallet(coin: Coin, account: Account, syncMode: SyncMode?): Wallet {
        return Wallet(coin, account, syncMode)
    }

}
