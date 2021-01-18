package io.horizontalsystems.bankwallet.modules.managewallets

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class ManageWalletsService(
        private val coinManager: ICoinManager,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager)
    : ManageWalletsModule.IManageWalletsService, Clearable {

    private val disposables = CompositeDisposable()
    private var wallets = mutableMapOf<Coin, Wallet>()

    override val stateAsync = BehaviorSubject.create<ManageWalletsModule.State>()

    override var state = ManageWalletsModule.State.empty()
        private set(value) {
            field = value
            stateAsync.onNext(value)
        }

    init {
        disposables.add(accountManager.accountsFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncState()
                })


        disposables.add(coinManager.coinAddedObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncState()
                })

        walletManager.walletsUpdatedObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { wallets ->
                    sync(wallets)
                }.let {
                    disposables.add(it)
                }

        sync(walletManager.wallets)
    }

    private fun sync(walletList: List<Wallet>) {
        wallets = mutableMapOf()
        walletList.forEach { wallet ->
            wallets[wallet.coin] = wallet
        }

        syncState()
    }

    override fun enable(coin: Coin, derivationSetting: DerivationSetting?) {
        val account = account(coin) ?: throw EnableCoinError.NoAccount

        val wallet = Wallet(coin, account)
        walletManager.save(listOf(wallet))
    }

    override fun disable(coin: Coin) {
        val wallet = wallets[coin] ?: return
        walletManager.delete(listOf(wallet))
    }

    override fun account(coin: Coin): Account? {
        return accountManager.account(coin.type)
    }

    override fun clear() {
        disposables.clear()
    }

    private fun syncState() {
        val featuredCoins = coinManager.featuredCoins
        val coins = coinManager.coins.filterNot { featuredCoins.contains(it) }

        state = ManageWalletsModule.State(
                featuredItems = featuredCoins.map { item(it) },
                items = coins.map { item(it) }
        )
    }

    private fun item(coin: Coin): ManageWalletsModule.Item {
        val hasWallet = wallets[coin] != null
        val hasAccount = account(coin) != null
        val state: ManageWalletsModule.ItemState = when {
            hasAccount -> ManageWalletsModule.ItemState.HasAccount(hasWallet)
            else -> ManageWalletsModule.ItemState.NoAccount
        }
        return ManageWalletsModule.Item(coin, state)
    }

    sealed class EnableCoinError : Exception() {
        object NoAccount : EnableCoinError()
    }
}
