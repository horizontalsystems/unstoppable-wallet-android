package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.LatestRate
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class BalanceService(
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    private val xRateManager: IRateManager,
    private val currencyManager: ICurrencyManager,
    private val localStorage: ILocalStorage,
    private val balanceSorter: BalanceSorter,
    private val connectivityManager: ConnectivityManager
) : Clearable {
    val networkAvailable: Boolean
        get() = connectivityManager.isConnected

    var sortType: BalanceSortType
        get() = localStorage.sortType
        set(value) {
            localStorage.sortType = value

            emitBalanceItems()
        }

    var balanceHidden: Boolean
        get() = localStorage.balanceHidden
        set(value) {
            localStorage.balanceHidden = value

            emitBalanceItems()
        }

    private val disposables = CompositeDisposable()
    private val latestRatesDisposables = CompositeDisposable()
    private val adaptersDisposables = CompositeDisposable()

    private val balanceItemsSubject = BehaviorSubject.create<Unit>()
    val balanceItemsObservable: Flowable<Unit> = balanceItemsSubject.toFlowable(BackpressureStrategy.DROP)

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    private var balanceItems = listOf<BalanceModule.BalanceItem>()
    val balanceItemsSorted: List<BalanceModule.BalanceItem>
        get() = balanceSorter.sort(balanceItems, sortType)

    init {
        refreshBalanceItems()

        walletManager.activeWalletsUpdatedObservable
            .subscribeIO {
                refreshBalanceItems()
            }
            .let {
                disposables.add(it)
            }

        adapterManager.adaptersReadyObservable
            .subscribeIO {
                unsubscribeFromAdapterUpdates()

                setDataFromAdapters()
                emitBalanceItems()

                subscribeForAdapterUpdates()
            }
            .let {
                disposables.add(it)
            }

        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                unsubscribeFromLatestRateUpdates()

                setLatestRates()
                emitBalanceItems()

                subscribeForLatestRateUpdates()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun refreshBalanceItems() {
        unsubscribeFromLatestRateUpdates()
        unsubscribeFromAdapterUpdates()

        rebuildBalanceItems()
        setLatestRates()
        setDataFromAdapters()
        emitBalanceItems()

        subscribeForLatestRateUpdates()
        subscribeForAdapterUpdates()
    }

    private fun setDataFromAdapters() {
        for (balanceItem in balanceItems) {
            val adapter = adapterManager.getBalanceAdapterForWallet(balanceItem.wallet) ?: continue

            balanceItem.balance = adapter.balance
            balanceItem.balanceLocked = adapter.balanceLocked
            balanceItem.state = adapter.balanceState
        }
    }

    private fun subscribeForAdapterUpdates() {
        for (balanceItem in balanceItems) {
            val adapter = adapterManager.getBalanceAdapterForWallet(balanceItem.wallet) ?: continue

            subscribeForBalanceUpdate(adapter, balanceItem)
            subscribeForStateUpdate(adapter, balanceItem)
        }
    }

    private fun unsubscribeFromAdapterUpdates() {
        adaptersDisposables.clear()
    }

    private fun subscribeForStateUpdate(adapter: IBalanceAdapter, balanceItem: BalanceModule.BalanceItem) {
        adapter.balanceStateUpdatedFlowable
            .subscribeIO {
                balanceItems.find { it == balanceItem }?.apply {
                    this.state = adapter.balanceState
                }

                emitBalanceItems()
            }
            .let {
                adaptersDisposables.add(it)
            }
    }

    private fun subscribeForBalanceUpdate(adapter: IBalanceAdapter, balanceItem: BalanceModule.BalanceItem) {
        adapter.balanceUpdatedFlowable
            .subscribeIO {
                balanceItems.find { it == balanceItem }?.apply {
                    this.balance = adapter.balance
                    this.balanceLocked = adapter.balanceLocked
                }

                emitBalanceItems()
            }
            .let {
                adaptersDisposables.add(it)
            }
    }

    private fun subscribeForLatestRateUpdates() {
        xRateManager.latestRateObservable(balanceItems.map { it.wallet.coin.type }, baseCurrency.code)
            .subscribeIO { latestRates: Map<CoinType, LatestRate> ->
                balanceItems.forEach { balanceItem ->
                    latestRates[balanceItem.wallet.coin.type]?.let {
                        balanceItem.latestRate = it
                    }
                }

                emitBalanceItems()
            }
            .let {
                latestRatesDisposables.add(it)
            }
    }

    private fun unsubscribeFromLatestRateUpdates() {
        latestRatesDisposables.clear()
    }

    private fun emitBalanceItems() {
        balanceItemsSubject.onNext(Unit)
    }

    private fun rebuildBalanceItems() {
        balanceItems = walletManager.activeWallets.map { wallet ->
            BalanceModule.BalanceItem(wallet, true)
        }
    }

    private fun setLatestRates() {
        balanceItems.forEach { balanceItem ->
            balanceItem.latestRate = xRateManager.latestRate(balanceItem.wallet.coin.type, baseCurrency.code)
        }
    }

    fun refresh() {
        adapterManager.refresh()
        xRateManager.refresh(baseCurrency.code)
    }

    override fun clear() {
        disposables.clear()
        adaptersDisposables.clear()
        latestRatesDisposables.clear()
    }

    fun refreshByWallet(wallet: Wallet) {
        adapterManager.refreshByWallet(wallet)
    }
}
