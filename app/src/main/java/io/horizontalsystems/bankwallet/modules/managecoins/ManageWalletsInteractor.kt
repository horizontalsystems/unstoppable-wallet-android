package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.WalletManager
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable

class ManageWalletsInteractor(private val appConfigProvider: IAppConfigProvider, private val walletManager: WalletManager, private val accountManager: IAccountManager, private val enabledCoinStorage: IEnabledWalletStorage)
    : ManageWalletsModule.IInteractor {

    var delegate: ManageWalletsModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    override fun load() {
        delegate?.didLoad(appConfigProvider.coins, walletManager.wallets)
    }

    override fun saveWallets(wallets: List<Wallet>) {
        val enabledCoins = mutableListOf<EnabledWallet>()
        wallets.forEachIndexed { order, wallet ->
            enabledCoins.add(EnabledWallet(wallet.coin.code, wallet.account.id, order, wallet.syncMode))
        }
        enabledCoinStorage.save(enabledCoins)
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
