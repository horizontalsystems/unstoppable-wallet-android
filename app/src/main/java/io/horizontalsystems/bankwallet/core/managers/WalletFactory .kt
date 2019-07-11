package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.IWalletFactory
import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.entities.Coin

class WalletFactory : IWalletFactory {

    override fun wallet(coin: Coin, account: Account): Wallet {
        return Wallet(coin, account, account.defaultSyncMode)
    }

}
