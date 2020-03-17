package io.horizontalsystems.bankwallet.modules.createwallet

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*

class CreateWalletInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val blockchainSettingsManager: IBlockchainSettingsManager
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

    override fun blockchainSettings(coinType: CoinType): BlockchainSetting? {
        return blockchainSettingsManager.blockchainSettingsForCreate(coinType)
    }

    override fun saveBlockchainSettings(blockchainSettings: BlockchainSetting) {
        blockchainSettingsManager.updateSettings(blockchainSettings)
    }
}
