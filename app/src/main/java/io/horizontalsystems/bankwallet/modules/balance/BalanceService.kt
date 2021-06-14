package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.LatestRate
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class BalanceService(
    private val accountManager: IAccountManager,
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    private val xRateManager: IRateManager,
    private val currencyManager: ICurrencyManager,
    private val localStorage: ILocalStorage,
    private val balanceSorter: BalanceSorter
) {
    var sortType: BalanceSortType
        get() = localStorage.sortType
        set(value) {
            localStorage.sortType = value

            emitBalanceItems()
        }

    private var disposables = CompositeDisposable()
    private val latestRatesDisposables = CompositeDisposable()
    private val adaptersDisposables = CompositeDisposable()

    private val activeAccountSubject = BehaviorSubject.create<Account>()
    val activeAccountObservable: Flowable<Account> =
        activeAccountSubject.toFlowable(BackpressureStrategy.DROP)

    private val balanceItemsSubject = BehaviorSubject.create<List<BalanceModule.BalanceItem>>()
    val balanceItemsObservable: Flowable<List<BalanceModule.BalanceItem>> =
        balanceItemsSubject.toFlowable(BackpressureStrategy.DROP)

    private var balanceHidden: Boolean
        get() = localStorage.balanceHidden
        set(value) {
            localStorage.balanceHidden = value

            balanceHiddenSubject.onNext(balanceHidden)
        }
    private val balanceHiddenSubject = BehaviorSubject.createDefault(balanceHidden)
    val balanceHiddenObservable: Flowable<Boolean> = balanceHiddenSubject.toFlowable(BackpressureStrategy.DROP)

    val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    private var balanceItems = listOf<BalanceModule.BalanceItem>()
    private var expandedWallet = Optional.empty<Wallet>()
    private val expandedWalletSubject = BehaviorSubject.createDefault(expandedWallet)
    val expandedWalletObservable: Flowable<Optional<Wallet>> =
        expandedWalletSubject.toFlowable(BackpressureStrategy.DROP)

    init {
        refreshActiveAccount()
        refreshBalanceItems()

        accountManager.activeAccountObservable
            .subscribeIO {
                refreshActiveAccount()
            }
            .let {
                disposables.add(it)
            }

        accountManager.accountsFlowable
            .subscribeIO {
                refreshActiveAccount()
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

        walletManager.activeWalletsUpdatedObservable
            .subscribeIO {
                refreshBalanceItems()
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
        balanceItemsSubject.onNext(balanceSorter.sort(balanceItems, sortType))
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

    private fun refreshActiveAccount() {
        accountManager.activeAccount?.let {
            activeAccountSubject.onNext(it)
        }
    }

    fun refresh() {
        adapterManager.refresh()
        xRateManager.refresh(baseCurrency.code)
    }

    fun toggleBalanceVisibility() {
        balanceHidden = !balanceHidden
    }

    fun toggleExpanded(wallet: Wallet) {
        val currentExpanded = expandedWallet.orElse(null)

        expandedWallet = when {
            wallet == currentExpanded -> Optional.empty()
            else -> Optional.of(wallet)
        }

        expandedWalletSubject.onNext(expandedWallet)
    }

}
