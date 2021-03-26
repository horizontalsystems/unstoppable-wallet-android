package io.horizontalsystems.bankwallet.modules.managewallets

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsService
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsService
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import kotlin.Comparator

class ManageWalletsService(
        private val coinManager: ICoinManager,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager,
        private val enableCoinsService: EnableCoinsService,
        private val blockchainSettingsService: BlockchainSettingsService)
    : ManageWalletsModule.IManageWalletsService, Clearable {

    val enableCoinAsync = PublishSubject.create<Coin>()
    val cancelEnableCoinAsync = PublishSubject.create<Coin>()

    private val disposables = CompositeDisposable()

    private var featuredCoins = listOf<Coin>()
    private var coins = listOf<Coin>()
    private var addedCoins = mutableListOf<Coin>()
    private var wallets = mutableMapOf<Coin, Wallet>()
    private var coinToEnable: Coin? = null
    private var resyncCoins = false

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
                    handleAccountsChanged()
                })

        disposables.add(coinManager.coinAddedObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    handleAdded(it)
                })

        walletManager.walletsUpdatedObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { wallets ->
                    handleUpdated(wallets)
                }.let {
                    disposables.add(it)
                }

        enableCoinsService.enableCoinsAsync
                .subscribeOn(Schedulers.io())
                .subscribe { coins ->
                    enable(coins)
                }.let {
                    disposables.add(it)
                }

        blockchainSettingsService.approveEnableCoinAsync
                .subscribeOn(Schedulers.io())
                .subscribe { coin ->
                    handleApproveEnable(coin)
                }.let {
                    disposables.add(it)
                }

        blockchainSettingsService.rejectEnableCoinAsync
                .subscribeOn(Schedulers.io())
                .subscribe { coin ->
                    cancelEnableCoinAsync.onNext(coin)
                }.let {
                    disposables.add(it)
                }

        sync(walletManager.wallets)
        syncCoins()
        syncState()
    }

    private fun handleAdded(coin: Coin) {
        addedCoins.add(coin)
        syncCoins()
        syncState()
    }

    private fun enable(coins: List<Coin>) {
        val nonEnabledCoins = coins.filter {
            !wallets.keys.contains(it)
        }

        walletManager.save(nonEnabledCoins.mapNotNull { wallet(it) })
    }

    private fun wallet(coin: Coin): Wallet? {
        val itemState = state.item(coin)?.state ?: return null
        if (itemState is ManageWalletsModule.ItemState.HasAccount) {
            return Wallet(coin, itemState.account)
        }

        return null
    }

    private fun handleApproveEnable(coin: Coin) {
        enable(listOf(coin))

        val account = account(coin)
        if (account == null || account.origin != AccountOrigin.Restored) {
            return
        }

        enableCoinsService.handle(coin.type, account.type)
    }

    private fun handleAccountsChanged() {
        val toEnable = coinToEnable ?: return

        enableCoinAsync.onNext(toEnable)
        enable(toEnable)
        coinToEnable = null
    }

    private fun handleUpdated(wallets: List<Wallet>) {
        sync(wallets)

        if (resyncCoins) {
            syncCoins()
            resyncCoins = false
        }

        syncState()
    }

    private fun syncCoins() {
        val (featuredCoins, regularCoins) = coinManager.groupedCoins

        this.featuredCoins = featuredCoins

        coins = regularCoins.sortedWith(Comparator{ lhsCoin, rhsCoin ->
            val lhsAdded = addedCoins.contains(lhsCoin)
            val rhsAdded = addedCoins.contains(rhsCoin)
            if (lhsAdded != rhsAdded) {
                return@Comparator if (lhsAdded) -1 else 1
            }

            val lhsEnabled = wallets[lhsCoin] != null
            val rhsEnabled = wallets[rhsCoin] != null

            if (lhsEnabled != rhsEnabled) {
                return@Comparator if (lhsEnabled) -1 else 1
            }

            return@Comparator lhsCoin.title.toLowerCase(Locale.ENGLISH).compareTo(rhsCoin.title.toLowerCase(Locale.ENGLISH))
        })

    }

    private fun sync(walletList: List<Wallet>) {
        wallets = mutableMapOf()
        walletList.forEach { wallet ->
            wallets[wallet.coin] = wallet
        }

        syncState()
    }

    override fun enable(coin: Coin, derivationSetting: DerivationSetting?) {
        val state = state.item(coin)?.state
        if (state == null || state !is ManageWalletsModule.ItemState.HasAccount) {
            return
        }

        blockchainSettingsService.approveEnable(coin, state.account.origin)
    }

    override fun disable(coin: Coin) {
        val wallet = wallets[coin] ?: return
        walletManager.delete(listOf(wallet))
    }

    override fun storeCoinToEnable(coin: Coin) {
        coinToEnable = coin
    }

    override fun account(coin: Coin): Account? {
        return accountManager.account(coin.type)
    }

    override fun clear() {
        disposables.clear()
    }

    private fun syncState() {
        state = ManageWalletsModule.State(
                featuredItems = featuredCoins.map { item(it) },
                items = coins.map { item(it) }
        )
    }

    private fun item(coin: Coin): ManageWalletsModule.Item {
        val account = account(coin)
        val state: ManageWalletsModule.ItemState = if (account == null) {
            ManageWalletsModule.ItemState.NoAccount
        } else {
            ManageWalletsModule.ItemState.HasAccount(account, wallets[coin] != null)
        }

        return ManageWalletsModule.Item(coin, state)
    }

    sealed class EnableCoinError : Exception() {
        object NoAccount : EnableCoinError()
    }
}
