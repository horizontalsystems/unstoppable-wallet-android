package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.FeaturedCoin

class CreateWalletInteractor(private val appConfigProvider: IAppConfigProvider) : CreateWalletModule.IInteractor {
    override val featuredCoins: List<FeaturedCoin>
        get() = appConfigProvider.featuredCoins

    override fun createWallet(coins: List<Coin>) {
        TODO("not implemented")
    }
}
