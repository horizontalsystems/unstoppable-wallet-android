package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*

class CreateWalletInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val coinSettingsManager: ICoinSettingsManager
) : CreateWalletModule.IInteractor {

    override val coins: List<Coin>
        get() = appConfigProvider.coins

    override val featuredCoins: List<Coin>
        get() = appConfigProvider.featuredCoins

    override fun createAccounts(accounts: List<Account>) {
        accounts.forEach {
            accountManager.save(it)
        }
    }

    override fun saveWallets(wallets: List<Wallet>) {
        walletManager.save(wallets)
    }

    @Throws
    override fun account(predefinedAccountType: PredefinedAccountType): Account {
        return accountCreator.newAccount(predefinedAccountType)
    }

    override fun coinSettingsToRequest(coin: Coin, accountOrigin: AccountOrigin): CoinSettings {
        return coinSettingsManager.coinSettingsToRequest(coin, accountOrigin)
    }

    override fun coinSettingsToSave(coin: Coin, accountOrigin: AccountOrigin, requestedCoinSettings: CoinSettings): CoinSettings {
        return coinSettingsManager.coinSettingsToSave(coin, accountOrigin, requestedCoinSettings)
    }
}
