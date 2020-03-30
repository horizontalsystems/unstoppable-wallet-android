package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation

class RestoreInteractor(
        private val accountCreator: IAccountCreator,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val blockchainSettingsManager: IBlockchainSettingsManager
) : RestoreModule.IInteractor {


    override fun createAccounts(accounts: List<Account>) {
        accounts.forEach {
            accountManager.save(it)
        }
    }

    override fun saveWallets(wallets: List<Wallet>) {
        walletManager.save(wallets)
    }

    override fun derivation(coinType: CoinType): Derivation? {
        return blockchainSettingsManager.derivationSetting(coinType)?.derivation
    }

    override fun syncMode(coinType: CoinType): SyncMode? {
        return blockchainSettingsManager.syncModeSetting(coinType)?.syncMode
    }

    override fun saveDerivation(coinType: CoinType, derivation: Derivation) {
        blockchainSettingsManager.updateSetting(DerivationSetting(coinType, derivation))
    }

    override fun saveSyncMode(coinType: CoinType, syncMode: SyncMode) {
        blockchainSettingsManager.updateSetting(SyncModeSetting(coinType, syncMode))
    }

    @Throws
    override fun account(accountType: AccountType): Account {
        return accountCreator.restoredAccount(accountType)
    }

    override fun create(account: Account) {
        accountManager.save(account)
    }

}
