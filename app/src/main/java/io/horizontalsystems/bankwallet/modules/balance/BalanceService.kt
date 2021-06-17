package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.core.managers.ConnectivityManager
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.LatestRate
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.CopyOnWriteArraySet

class BalanceService(
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    private val xRateManager: IRateManager,
    private val currencyManager: ICurrencyManager,
    private val localStorage: ILocalStorage,
    private val balanceSorter: BalanceSorter,
    private val connectivityManager: ConnectivityManager,
    private val feeCoinProvider: FeeCoinProvider,
    private val accountSettingManager: AccountSettingManager
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

    private var balanceItems = CopyOnWriteArraySet<BalanceModule.BalanceItem>()
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

        accountSettingManager.ethereumNetworkObservable
            .subscribeIO {
                refreshBalanceItems()
            }
            .let {
                disposables.add(it)
            }

        accountSettingManager.binanceSmartChainNetworkObservable
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
        val coinTypes = balanceItems.map { it.wallet.coin.type }

        // the send module needs the fee coin rate synchronous
        // that is why here we request fee coins too
        // todo: need to find a better solution
        val feeCoinTypes = coinTypes.mapNotNull { feeCoinProvider.feeCoinType(it) }
        val allCoinTypes = (coinTypes + feeCoinTypes).distinct()

        xRateManager.latestRateObservable(allCoinTypes, baseCurrency.code)
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
        balanceItems.clear()
        balanceItems.addAll(walletManager.activeWallets.map { wallet ->
            BalanceModule.BalanceItem(wallet, isMainNet(wallet))
        })
    }

    private fun isMainNet(wallet: Wallet) = when (wallet.coin.type) {
        is CoinType.Ethereum,
        is CoinType.Erc20 -> {
            accountSettingManager.ethereumNetwork(wallet.account).networkType.isMainNet
        }
        is CoinType.BinanceSmartChain -> {
            accountSettingManager.binanceSmartChainNetwork(wallet.account).networkType.isMainNet
        }
        else -> true
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
