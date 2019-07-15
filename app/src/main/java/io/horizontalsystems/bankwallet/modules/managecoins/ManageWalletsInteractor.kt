package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.entities.*
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val accountCreator: IAccountCreator,
        private val walletCreator: IWalletCreator,
        private val walletManager: WalletManager,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager)
    : ManageWalletsModule.IInteractor {

    var delegate: ManageWalletsModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    override fun load() {
        delegate?.didLoad(appConfigProvider.coins, walletManager.wallets)
    }

    override fun saveWallets(wallets: List<Wallet>) {
        walletManager.enable(wallets)
        delegate?.didSaveChanges()
    }

    override fun createWalletForCoin(coin: Coin): Wallet {
        val defaultAccountType = when (coin.type) {
            is CoinType.Bitcoin,
            is CoinType.BitcoinCash,
            is CoinType.Dash,
            is CoinType.Ethereum,
            is CoinType.Erc20 -> Words12AccountType()
            else -> throw Exception("New wallet creation is not supported for coin: ${coin.code}")
        }

        val account = predefinedAccountTypeManager.createAccount(defaultAccountType)
        if (account == null) {
            throw Exception("New wallet creation is not supported for coin: ${coin.code}")
        }

        return walletCreator.wallet(coin, account)
    }

    override fun restoreWallet(coin: Coin, accountType: AccountType, syncMode: SyncMode): Wallet {
        val account = accountCreator.createRestoredAccount(accountType, syncMode)
        return walletCreator.wallet(coin, account)
    }

    override fun clear() {
        disposables.clear()
    }

}
