package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.reactivex.disposables.Disposable

class ManageWalletsInteractor(
        private val coinManager: ICoinManager,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager,
        private val accountCreator: IAccountCreator,
        private val blockchainSettingsManager: IBlockchainSettingsManager
) : ManageWalletsModule.IInteractor {

    var delegate: ManageWalletsModule.InteractorDelegate? = null
    private var disposable: Disposable? = null

    override val coins: List<Coin>
        get() = coinManager.coins

    override val featuredCoins: List<Coin>
        get() = coinManager.featuredCoins

    override val wallets: List<Wallet>
        get() = walletManager.wallets

    override val accounts: List<Account>
        get() = accountManager.accounts

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

    override fun derivationSetting(coinType: CoinType): DerivationSetting? {
        return blockchainSettingsManager.derivationSetting(coinType)
    }

    override fun saveDerivationSetting(derivationSetting: DerivationSetting) {
        blockchainSettingsManager.saveSetting(derivationSetting)
    }

    override fun initializeSettingsWithDefault(coinType: CoinType) {
        blockchainSettingsManager.initializeSettingsWithDefault(coinType)
    }

    override fun initializeSettings(coinType: CoinType) {
        blockchainSettingsManager.initializeSettings(coinType)
    }

    override fun subscribeForNewTokenAddition() {
        disposable = coinManager.coinAddedObservable
                .subscribe({ delegate?.onNewTokenAdded() }, { /* error */})
    }

    override fun clear() {
        disposable?.dispose()
        disposable = null
    }
}
