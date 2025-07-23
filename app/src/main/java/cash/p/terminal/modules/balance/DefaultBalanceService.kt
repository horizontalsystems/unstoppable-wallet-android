package cash.p.terminal.modules.balance

import cash.p.terminal.core.App
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.isNative
import cash.p.terminal.core.managers.ConnectivityManager
import cash.p.terminal.wallet.Account
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
            // Re-sort current list
            updateBalanceItems { currentItems ->
                currentItems // just return current items, sorting will happen automatically
            }
        }

    private var _isWatchAccount = false

    override val isWatchAccount: Boolean
        get() = _isWatchAccount

    override val account: Account?
        get() = accountManager.activeAccount

    private var hideZeroBalances = false
    private var started: Boolean = false

    // Replace CopyOnWriteArrayList with StateFlow
    private val _balanceItemsState = MutableStateFlow<List<BalanceItem>>(emptyList())
    override val balanceItemsFlow = _balanceItemsState.asStateFlow()

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

    /**
     * Safe balance list update with automatic sorting and filtering
     */
    private fun updateBalanceItems(
        transform: (current: List<BalanceItem>) -> List<BalanceItem>
    ) {
        _balanceItemsState.update { currentItems ->
            val updatedItems = transform(currentItems)
            val sortedItems = balanceSorter.sort(updatedItems, sortType)
            getFilteredItems(sortedItems)
        }
    }

    /**
     * Applies filtering for zero balances
     */
    private fun getFilteredItems(items: List<BalanceItem>): List<BalanceItem> {
        return if (hideZeroBalances) {
            items.filter { it.wallet.token.type.isNative || it.balanceData.total > BigDecimal.ZERO }
        } else {
            items
        }
    }

    private fun handleAdaptersReady() {
        updateBalanceItems { currentItems ->
            currentItems.map { balanceItem ->
                balanceItem.copy(
                    balanceData = adapterRepository.balanceData(balanceItem.wallet),
                    state = adapterRepository.state(balanceItem.wallet),
                    sendAllowed = adapterRepository.sendAllowed(balanceItem.wallet),
                )
            }
        }
    }

    private fun handleAdapterUpdate(wallet: Wallet) {
        updateBalanceItems { currentItems ->
            currentItems.map { item ->
                if (item.wallet == wallet) {
                    item.copy(
                        balanceData = adapterRepository.balanceData(wallet),
                        state = adapterRepository.state(wallet),
                        sendAllowed = adapterRepository.sendAllowed(wallet),
                    )
                } else {
                    item
                }
            }
        }
    }

    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        updateBalanceItems { currentItems ->
            currentItems.map { balanceItem ->
                if (latestRates.containsKey(balanceItem.wallet.coin.uid)) {
                    balanceItem.copy(coinPrice = latestRates[balanceItem.wallet.coin.uid])
                } else {
                    balanceItem
                }
            }
        }
    }

    private fun handleWalletsUpdate(wallets: List<Wallet>) {
        // Update account state
        _isWatchAccount = accountManager.activeAccount?.isWatchAccount == true
        hideZeroBalances = accountManager.activeAccount?.type?.hideZeroBalances == true

        // Configure repositories
        adapterRepository.setWallet(wallets)
        xRateRepository.setCoinUids(wallets.map { it.coin.uid })
        val latestRates = xRateRepository.getLatestRates()

        // Complete replacement of balance list
        updateBalanceItems { _ ->
            wallets.map { wallet ->
                BalanceItem(
                    wallet = wallet,
                    balanceData = adapterRepository.balanceData(wallet),
                    state = adapterRepository.state(wallet),
                    sendAllowed = adapterRepository.sendAllowed(wallet),
                    coinPrice = latestRates[wallet.coin.uid]
                )
            }
        }
    }

    override suspend fun refresh() {
        xRateRepository.refresh()
        adapterRepository.refresh()
    }

    override fun clear() {
        coroutineScope.cancel()
        adapterRepository.clear()
        started = false
        // Clear state
        _balanceItemsState.value = emptyList()
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