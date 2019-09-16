package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.Coin

class CreateWalletInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val accountCreator: IAccountCreator
) : CreateWalletModule.IInteractor {

    override val featuredCoins: List<Coin>
        get() = appConfigProvider.featuredCoins

    override fun createWallet(coin: Coin) {
        accountCreator.createNewAccount(coin)
    }
}
