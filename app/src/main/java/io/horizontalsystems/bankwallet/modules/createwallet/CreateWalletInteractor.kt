package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.Coin

class CreateWalletInteractor(private val appConfigProvider: IAppConfigProvider) : CreateWalletModule.IInteractor {
    override val featuredCoins: List<Coin>
        get() = appConfigProvider.featuredCoins

    override fun createWallet(coins: Coin) {
        TODO("not implemented")
    }
}
