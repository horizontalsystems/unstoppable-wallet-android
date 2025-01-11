package cash.p.terminal.modules.balance

import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.isNative
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.core.stats.statSortType
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.BalanceSortType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.balance.BalanceItem
import cash.p.terminal.wallet.balance.BalanceService
import cash.p.terminal.wallet.balance.BalanceXRateRepository
import cash.p.terminal.wallet.models.CoinPrice
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList

class DefaultBalanceService private constructor(
    private val activeWalletRepository: BalanceActiveWalletRepository,
    private val xRateRepository: BalanceXRateRepository,
    private val adapterRepository: BalanceAdapterRepository,
    private val localStorage: ILocalStorage,
    private val connectivityManager: ConnectivityManager,
    private val balanceSorter: BalanceSorter,
    private val accountManager: IAccountManager
) : BalanceService {

    override val networkAvailable by connectivityManager::isConnected
    override val baseCurrency by xRateRepository::baseCurrency

    override var sortType: BalanceSortType
        get() = localStorage.sortType
        set(value) {
            localStorage.sortType = value

            sortAndEmitItems()

            stat(page = StatPage.Balance, event = StatEvent.SwitchSortType(value.statSortType))
        }

    private var _isWatchAccount = false

    override val isWatchAccount: Boolean
        get() = _isWatchAccount

    override val account: Account?
        get() = accountManager.activeAccount

    private var hideZeroBalances = false

    private var started: Boolean = false

    private val allBalanceItems = CopyOnWriteArrayList<BalanceItem>()

    /* getBalanceItems should return new immutable list */
    private fun getBalanceItems(): List<BalanceItem> = if (hideZeroBalances) {
        allBalanceItems.filter { it.wallet.token.type.isNative || it.balanceData.total > BigDecimal.ZERO }
    } else {
        allBalanceItems.toList()
    }

    private val _balanceItemsFlow = MutableStateFlow<List<BalanceItem>?>(null)
    override val balanceItemsFlow = _balanceItemsFlow.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun start() {
        if (started) return
        started = true

        coroutineScope.launch {
            activeWalletRepository.itemsObservable.asFlow().collect { wallets ->
                handleWalletsUpdate(wallets)
            }
        }
        coroutineScope.launch {
            xRateRepository.itemObservable.asFlow().collect { latestRates ->
                handleXRateUpdate(latestRates)
            }
        }
        coroutineScope.launch {
            adapterRepository.readyObservable.asFlow().collect {
                handleAdaptersReady()
            }
        }
        coroutineScope.launch {
            adapterRepository.updatesObservable.asFlow().collect {
                handleAdapterUpdate(it)
            }
        }
    }

    @Synchronized
    private fun sortAndEmitItems() {
        val sorted = balanceSorter.sort(allBalanceItems, sortType)
        allBalanceItems.clear()
        allBalanceItems.addAll(sorted)

        _balanceItemsFlow.update {
            if (accountManager.activeAccount?.type is AccountType.Cex) {
                null
            } else {
                getBalanceItems()
            }
        }
    }

    @Synchronized
    private fun handleAdaptersReady() {
        for (i in 0 until allBalanceItems.size) {
            val balanceItem = allBalanceItems[i]

            allBalanceItems[i] = balanceItem.copy(
                balanceData = adapterRepository.balanceData(balanceItem.wallet),
                state = adapterRepository.state(balanceItem.wallet),
                sendAllowed = adapterRepository.sendAllowed(balanceItem.wallet),
            )
        }

        sortAndEmitItems()
    }

    @Synchronized
    private fun handleAdapterUpdate(wallet: Wallet) {
        val indexOfFirst = allBalanceItems.indexOfFirst { it.wallet == wallet }
        if (indexOfFirst != -1) {
            val itemToUpdate = allBalanceItems[indexOfFirst]

            allBalanceItems[indexOfFirst] = itemToUpdate.copy(
                balanceData = adapterRepository.balanceData(wallet),
                state = adapterRepository.state(wallet),
                sendAllowed = adapterRepository.sendAllowed(wallet),
            )

            sortAndEmitItems()
        }
    }

    @Synchronized
    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        for (i in 0 until allBalanceItems.size) {
            val balanceItem = allBalanceItems[i]

            if (latestRates.containsKey(balanceItem.wallet.coin.uid)) {
                allBalanceItems[i] =
                    balanceItem.copy(coinPrice = latestRates[balanceItem.wallet.coin.uid])
            }
        }

        sortAndEmitItems()
    }

    @Synchronized
    private fun handleWalletsUpdate(wallets: List<Wallet>) {
        _isWatchAccount = accountManager.activeAccount?.isWatchAccount == true
        hideZeroBalances = accountManager.activeAccount?.type?.hideZeroBalances == true

        adapterRepository.setWallet(wallets)
        xRateRepository.setCoinUids(wallets.map { it.coin.uid })
        val latestRates = xRateRepository.getLatestRates()

        val balanceItems = wallets.map { wallet ->
            BalanceItem(
                wallet = wallet,
                balanceData = adapterRepository.balanceData(wallet),
                state = adapterRepository.state(wallet),
                sendAllowed = adapterRepository.sendAllowed(wallet),
                coinPrice = latestRates[wallet.coin.uid]
            )
        }

        this.allBalanceItems.clear()
        this.allBalanceItems.addAll(balanceItems)

        sortAndEmitItems()
    }

    override suspend fun refresh() {
        xRateRepository.refresh()
        adapterRepository.refresh()
    }

    override fun clear() {
        coroutineScope.cancel()
        adapterRepository.clear()
        started = false
    }

    override val disabledWalletSubject = PublishSubject.create<Wallet>()
    override fun disable(wallet: Wallet) {
        activeWalletRepository.disable(wallet)

        disabledWalletSubject.onNext(wallet)
    }

    override fun enable(wallet: Wallet) {
        activeWalletRepository.enable(wallet)
    }

    companion object {
        fun getInstance(tag: String): DefaultBalanceService {
            return DefaultBalanceService(
                BalanceActiveWalletRepository(App.walletManager, App.evmSyncSourceManager),
                DefaultBalanceXRateRepository(tag, App.currencyManager, App.marketKit),
                BalanceAdapterRepository(
                    App.adapterManager,
                    BalanceCache(App.appDatabase.enabledWalletsCacheDao())
                ),
                App.localStorage,
                App.connectivityManager,
                BalanceSorter(),
                App.accountManager
            )

        }
    }
}
