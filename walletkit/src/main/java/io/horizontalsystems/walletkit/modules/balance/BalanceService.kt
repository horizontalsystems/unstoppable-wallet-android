package io.horizontalsystems.walletkit.modules.balance

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.isNative
import io.horizontalsystems.walletkit.core.managers.ConnectivityManager
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal

class BalanceService(
    private val activeWalletRepository: BalanceActiveWalletRepository,
    private val xRateRepository: BalanceXRateRepository,
    private val adapterRepository: BalanceAdapterRepository,
    private val localStorage: ILocalStorage,
    private val connectivityManager: ConnectivityManager,
    private val balanceSorter: BalanceSorter,
    private val accountManager: IAccountManager
) : Clearable {

    val networkAvailable by connectivityManager::isConnected
    val baseCurrency by xRateRepository::baseCurrency

    var sortType = localStorage.sortType
        private set

    var isWatchAccount = false
        private set

    val account: Account?
        get() = accountManager.activeAccount

    private var hideZeroBalances = false

    private var allBalanceItems = listOf<BalanceModule.BalanceItem>()

    private val mutex = Mutex()

    private val _balanceItemsFlow = MutableStateFlow<List<BalanceModule.BalanceItem>?>(null)
    val balanceItemsFlow = _balanceItemsFlow.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var sortAndEmitJob: Job? = null

    fun start() {
        coroutineScope.launch {
            activeWalletRepository.itemsFlow.collect { wallets ->
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

    fun setSortType(value: BalanceSortType) {
        sortType = value
        localStorage.sortType = value

        sortAndEmitItems()
    }

    private fun sortAndEmitItems() {
        sortAndEmitJob?.cancel()
        sortAndEmitJob = coroutineScope.launch {
            // Sort into a local — never write the result back to allBalanceItems.
            // The sort runs outside the mutex and is cancelled by the next update;
            // a cancelled job writing its (already stale) sorted list back would
            // overwrite a newer state that arrived mid-sort, and the lost update
            // leaves a row frozen (e.g. permanently "Syncing"/"Sync error"),
            // because an unchanged adapter state never emits again.
            val sortedItems = balanceSorter.sort(allBalanceItems, sortType)

            // Publish under the mutex: updates cancel this job while holding it,
            // so checking ensureActive inside the lock guarantees a cancelled job
            // can never emit its stale rows after the replacement job's fresh ones.
            mutex.withLock {
                ensureActive()

                _balanceItemsFlow.update {
                    if (hideZeroBalances) {
                        sortedItems.filter { it.wallet.token.type.isNative || it.balanceData.total > BigDecimal.ZERO }
                    } else {
                        sortedItems
                    }
                }
            }
        }
    }

    private suspend fun handleAdaptersReady() = mutex.withLock {
        val allBalanceItems = this.allBalanceItems.toMutableList()
        for (i in 0 until allBalanceItems.size) {
            val balanceItem = allBalanceItems[i]

            allBalanceItems[i] = balanceItem.copy(
                balanceData = adapterRepository.balanceData(balanceItem.wallet),
                state = adapterRepository.state(balanceItem.wallet),
                warning = adapterRepository.warning(balanceItem.wallet)
            )
        }

        this.allBalanceItems = allBalanceItems

        sortAndEmitItems()
    }

    private suspend fun handleAdapterUpdate(wallet: Wallet) = mutex.withLock {
        val indexOfFirst = allBalanceItems.indexOfFirst { it.wallet == wallet }
        if (indexOfFirst != -1) {
            val allBalanceItems = allBalanceItems.toMutableList()
            val itemToUpdate = allBalanceItems[indexOfFirst]

            allBalanceItems[indexOfFirst] = itemToUpdate.copy(
                balanceData = adapterRepository.balanceData(wallet),
                state = adapterRepository.state(wallet),
                warning = adapterRepository.warning(wallet)
            )

            this.allBalanceItems = allBalanceItems

            sortAndEmitItems()
        }
    }

    private suspend fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) = mutex.withLock {
        val allBalanceItems = allBalanceItems.toMutableList()
        for (i in 0 until allBalanceItems.size) {
            val balanceItem = allBalanceItems[i]

            if (latestRates.containsKey(balanceItem.wallet.coin.uid)) {
                allBalanceItems[i] = balanceItem.copy(coinPrice = latestRates[balanceItem.wallet.coin.uid])
            }
        }

        this.allBalanceItems = allBalanceItems

        sortAndEmitItems()
    }

    private suspend fun handleWalletsUpdate(wallets: List<Wallet>) = mutex.withLock {
        isWatchAccount = accountManager.activeAccount?.isWatchAccount == true
        hideZeroBalances = accountManager.activeAccount?.type?.hideZeroBalances == true

        adapterRepository.setWallet(wallets)
        xRateRepository.setCoinUids(wallets.map { it.coin.uid })
        val latestRates = xRateRepository.getLatestRates()

        val balanceItems = wallets.map { wallet ->
            BalanceModule.BalanceItem(
                wallet = wallet,
                balanceData = adapterRepository.balanceData(wallet),
                state = adapterRepository.state(wallet),
                coinPrice = latestRates[wallet.coin.uid]
            )
        }

        allBalanceItems = balanceItems

        sortAndEmitItems()
    }

    suspend fun refresh() {
        xRateRepository.refresh()
        adapterRepository.refresh()
    }

    override fun clear() {
        coroutineScope.cancel()
        adapterRepository.clear()
    }

    fun disable(wallet: Wallet) {
        activeWalletRepository.disable(wallet)
    }

    fun enable(wallet: Wallet) {
        activeWalletRepository.enable(wallet)
    }

    companion object {
        fun getInstance(tag: String): BalanceService {
            return BalanceService(
                BalanceActiveWalletRepository(App.walletManager, App.evmSyncSourceManager),
                BalanceXRateRepository(tag, App.currencyManager, App.marketKit),
                BalanceAdapterRepository(App.adapterManager, BalanceCache(App.appDatabase.enabledWalletsCacheDao())),
                App.localStorage,
                App.connectivityManager,
                BalanceSorter(),
                App.accountManager
            )

        }
    }
}
