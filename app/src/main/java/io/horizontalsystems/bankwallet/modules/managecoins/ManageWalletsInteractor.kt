package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.Wallet
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.entities.CoinType
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsInteractor(
        private val appConfigProvider: IAppConfigProvider,
        private val walletManager: WalletManager,
        private val accountManager: IAccountManager)
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

    override fun accounts(coinType: CoinType): Flowable<List<Account>> {
        return accountManager.accountsFlowable.map { accountsList ->
            accountsList.filter { coinType.canSupport(it.type) }
        }
    }

    override fun clear() {
        disposables.clear()
    }

}
