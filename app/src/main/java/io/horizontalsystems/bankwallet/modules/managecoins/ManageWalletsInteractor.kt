package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val walletManager: IWalletManager,
        private val accountCreator: IAccountCreator,
        private val walletFactory: IWalletFactory)
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

    override fun createWallet(coin: Coin): Wallet {
        val account = accountCreator.createNewAccount(coin.type.defaultAccountType)

        return walletFactory.wallet(coin, account, account.defaultSyncMode)
    }

    override fun restoreWallet(coin: Coin, accountType: AccountType, syncMode: SyncMode): Wallet {
        val account = accountCreator.createRestoredAccount(accountType, syncMode)

        return walletFactory.wallet(coin, account, syncMode)
    }

    override fun clear() {
        disposables.clear()
    }
}
