package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statSortType
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.CoinPrice
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

    var sortType: BalanceSortType
        get() = localStorage.sortType
        set(value) {
            localStorage.sortType = value

            sortAndEmitItems()

            stat(page = StatPage.Balance, event = StatEvent.SwitchSortType(value.statSortType))
        }

    var isWatchAccount = false
        private set

    val account: Account?
        get() = accountManager.activeAccount

    private var hideZeroBalances = false

    private val allBalanceItems = CopyOnWriteArrayList<BalanceModule.BalanceItem>()

    /* getBalanceItems should return new immutable list */
    private fun getBalanceItems(): List<BalanceModule.BalanceItem> = if (hideZeroBalances) {
        allBalanceItems.filter { it.wallet.token.type.isNative || it.balanceData.total > BigDecimal.ZERO }
    } else {
        allBalanceItems.toList()
    }

    private val _balanceItemsFlow = MutableStateFlow<List<BalanceModule.BalanceItem>?>(null)
    val balanceItemsFlow = _balanceItemsFlow.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {
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
            getBalanceItems()
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
                allBalanceItems[i] = balanceItem.copy(coinPrice = latestRates[balanceItem.wallet.coin.uid])
            }
        }

        sortAndEmitItems()
    }

    @Synchronized
    private fun handleWalletsUpdate(wallets: List<Wallet>) {
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
                sendAllowed = adapterRepository.sendAllowed(wallet),
                coinPrice = latestRates[wallet.coin.uid]
            )
        }

        this.allBalanceItems.clear()
        this.allBalanceItems.addAll(balanceItems)

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

    val disabledWalletSubject = PublishSubject.create<Wallet>()
    fun disable(wallet: Wallet) {
        activeWalletRepository.disable(wallet)

        disabledWalletSubject.onNext(wallet)
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
