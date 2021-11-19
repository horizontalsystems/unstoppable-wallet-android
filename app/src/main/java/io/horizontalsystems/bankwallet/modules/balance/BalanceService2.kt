package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList

class BalanceService2(
    private val activeWalletRepository: BalanceActiveWalletRepository,
    private val xRateRepository: BalanceXRateRepository,
    private val adapterRepository: BalanceAdapterRepository,
    private val networkTypeChecker: NetworkTypeChecker,
    private val localStorage: ILocalStorage,
    private val connectivityManager: ConnectivityManager,
    private val balanceSorter: BalanceSorter
) : Clearable {

    val networkAvailable by connectivityManager::isConnected
    val baseCurrency by xRateRepository::baseCurrency
    var balanceHidden by localStorage::balanceHidden

    var sortType: BalanceSortType
        get() = localStorage.sortType
        set(value) {
            localStorage.sortType = value

            sortAndEmitItems()
        }

    val balanceItems = CopyOnWriteArrayList<BalanceModule.BalanceItem>()

    private val balanceItemsSubject = PublishSubject.create<Unit>()
    val balanceItemsObservable: Observable<Unit> get() = balanceItemsSubject

    private val disposables = CompositeDisposable()

    init {
        activeWalletRepository.itemsObservable
            .subscribeIO { wallets ->
                handleWalletsUpdate(wallets)
            }
            .let {
                disposables.add(it)
            }

        xRateRepository.itemObservable
            .subscribeIO { latestRates ->
                handleXRateUpdate(latestRates)
            }
            .let {
                disposables.add(it)
            }

        adapterRepository.readyObservable
            .subscribeIO {
                handleAdaptersReady()
            }
            .let {
                disposables.add(it)
            }

        adapterRepository.updatesObservable
            .subscribeIO {
                handleAdapterUpdate(it)
            }
            .let {
                disposables.add(it)
            }

    }

    private fun sortAndEmitItems() {
        val sorted = balanceSorter.sort(balanceItems, sortType)
        balanceItems.clear()
        balanceItems.addAll(sorted)

        balanceItemsSubject.onNext(Unit)
    }

    @Synchronized
    fun handleAdaptersReady() {
        for (i in 0 until balanceItems.size) {
            val balanceItem = balanceItems[i]

            balanceItems[i] = balanceItem.copy(
                balanceData = adapterRepository.balanceData(balanceItem.wallet),
                state = adapterRepository.state(balanceItem.wallet)
            )
        }

        sortAndEmitItems()
    }

    @Synchronized
    private fun handleAdapterUpdate(wallet: Wallet) {
        val indexOfFirst = balanceItems.indexOfFirst { it.wallet == wallet }
        if (indexOfFirst != -1) {
            val itemToUpdate = balanceItems[indexOfFirst]

            balanceItems[indexOfFirst] = itemToUpdate.copy(
                balanceData = adapterRepository.balanceData(wallet),
                state = adapterRepository.state(wallet)
            )

            sortAndEmitItems()
        }
    }

    @Synchronized
    private fun handleXRateUpdate(latestRates: Map<String, CoinPrice?>) {
        for (i in 0 until balanceItems.size) {
            val balanceItem = balanceItems[i]

            if (latestRates.containsKey(balanceItem.wallet.coin.uid)) {
                balanceItems[i] = balanceItem.copy(coinPrice = latestRates[balanceItem.wallet.coin.uid])
            }
        }

        sortAndEmitItems()
    }

    @Synchronized
    private fun handleWalletsUpdate(wallets: List<Wallet>) {
        adapterRepository.setWallet(wallets)
        xRateRepository.setCoinUids(wallets.mapNotNull { if (it.coin.isCustom) null else it.coin.uid })
        val latestRates = xRateRepository.getLatestRates()

        val balanceItems = wallets.map { wallet ->
            BalanceModule.BalanceItem(
                wallet,
                networkTypeChecker.isMainNet(wallet),
                adapterRepository.balanceData(wallet),
                adapterRepository.state(wallet),
                latestRates[wallet.coin.uid]
            )
        }

        this.balanceItems.clear()
        this.balanceItems.addAll(balanceItems)

        sortAndEmitItems()
    }

    fun refresh() {
        xRateRepository.refresh()
        adapterRepository.refresh()
    }

    fun refreshByWallet(wallet: Wallet) {
        adapterRepository.refreshByWallet(wallet)
    }

    override fun clear() {
        disposables.clear()
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
}
