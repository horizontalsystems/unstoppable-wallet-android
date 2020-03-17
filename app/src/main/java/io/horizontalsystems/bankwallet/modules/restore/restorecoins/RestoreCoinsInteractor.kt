package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*

class RestoreCoinsInteractor(
        private val appConfigProvider: IAppConfigProvider
) : RestoreCoinsModule.IInteractor {

    override val coins: List<Coin>
        get() = appConfigProvider.coins

    override val featuredCoins: List<Coin>
        get() = appConfigProvider.featuredCoins

}
