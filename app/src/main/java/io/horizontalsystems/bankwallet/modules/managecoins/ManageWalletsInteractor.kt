package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*

class ManageWalletsInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager,
        private val accountCreator: IAccountCreator,
        private val coinSettingsManager: ICoinSettingsManager
) : ManageWalletsModule.IInteractor {

    override val coins: List<Coin>
        get() = appConfigProvider.coins

    override val featuredCoins: List<Coin>
        get() = appConfigProvider.featuredCoins

    override val wallets: List<Wallet>
        get() = walletManager.wallets

    override val accounts: List<Account>
        get() = accountManager.accounts

    override fun coinSettingsToSave(coin: Coin, origin: AccountOrigin, requestedCoinSettings: MutableMap<CoinSetting, String>): CoinSettings {
        return coinSettingsManager.coinSettingsToSave(coin, origin, requestedCoinSettings)
    }

    override fun coinSettingsToRequest(coin: Coin, origin: AccountOrigin): CoinSettings {
        return coinSettingsManager.coinSettingsToRequest(coin, origin)
    }

    override fun save(wallet: Wallet) {
        walletManager.save(listOf(wallet))
    }

    override fun save(account: Account) {
        accountManager.save(account)
    }

    override fun delete(wallet: Wallet) {
        walletManager.delete(listOf(wallet))
    }

    override fun createAccount(predefinedAccountType: PredefinedAccountType): Account {
        return accountCreator.newAccount(predefinedAccountType)
    }

    override fun createRestoredAccount(accountType: AccountType): Account {
        return accountCreator.restoredAccount(accountType)
    }

}
