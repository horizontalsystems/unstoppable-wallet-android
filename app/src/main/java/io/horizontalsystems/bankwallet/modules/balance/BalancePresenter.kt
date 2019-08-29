package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.core.managers.RateStatsManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.BalanceItem
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class BalancePresenter(
        private val interactor: BalanceModule.IInteractor,
        private val router: BalanceModule.IRouter,
        private val dataSource: BalanceModule.DataSource,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager,
        private val factory: BalanceViewItemFactory)
    : BalanceModule.IViewDelegate, BalanceModule.IInteractorDelegate {

    var view: BalanceModule.IView? = null

    private val disposables = CompositeDisposable()
    private val flushSubject = PublishSubject.create<Unit>()
    private val showSortingButtonThreshold = 5
    private var accountToBackup: Account? = null

    // ViewDelegate

    override val itemsCount: Int
        get() = dataSource.count

    override fun viewDidLoad() {
        interactor.initWallets()

        flushSubject
                .debounce(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { updateViewItems() }
                .subscribe()?.let { disposables.add(it) }

        Flowable.interval(1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    dataSource.items.mapNotNull { it.rate }.forEach {
                        didUpdateRate(it)
                    }
                }.let { disposables.add(it) }
    }

    override fun getViewItem(position: Int) =
            factory.createViewItem(dataSource.getItem(position), dataSource.currency)

    override fun getHeaderViewItem() =
            factory.createHeaderViewItem(dataSource.items, dataSource.chartEnabled, dataSource.currency)

    override fun refresh() {
        interactor.refresh()
    }

    override fun onReceive(position: Int) {
        val account = dataSource.getItem(position).wallet.account
        if (account.isBackedUp) {
            router.openReceiveDialog(dataSource.getItem(position).wallet)
        } else {
            accountToBackup = account
            view?.showBackupAlert()
        }
    }

    override fun onPay(position: Int) {
        router.openSendDialog(dataSource.getItem(position).wallet)
    }

    override fun onClear() {
        interactor.clear()
        disposables.clear()
    }

    override fun openManageCoins() {
        router.openManageCoins()
    }

    override fun onSortClick() {
        router.openSortTypeDialog(interactor.getSortingType())
    }

    override fun onEnableChart(enabled: Boolean) {
        dataSource.chartEnabled = enabled

        if (enabled) {
            dataSource.coinCodes.forEach {
                interactor.fetchRateStats(dataSource.currency.code, it)
            }
        } else {
            view?.reload()
        }
    }

    override fun onSortTypeChanged(sortType: BalanceSortType) {
        interactor.saveSortingType(sortType)
        dataSource.sortBy(sortType)
        view?.reload()
    }

    // InteractorDelegate

    override fun didUpdateWallets(wallets: List<Wallet>) {
        val balanceItems = wallets.map { wallet ->
            val adapter = interactor.getBalanceAdapterForWallet(wallet)
            val adapterState = adapter?.state ?: AdapterState.NotReady

            val balanceItem = BalanceItem(wallet, adapter?.balance ?: BigDecimal.ZERO, adapterState)
            if (dataSource.chartEnabled) {
                balanceItem.chartStatsFetching = true
                interactor.fetchRateStats(dataSource.currency.code, wallet.coin.code)
            }

            balanceItem
        }

        dataSource.set(balanceItems)
        interactor.fetchRates(dataSource.currency.code, dataSource.coinCodes)

        view?.setSortingOn(balanceItems.size >= showSortingButtonThreshold)
        view?.reload()
    }

    override fun didUpdateCurrency(currency: Currency) {
        dataSource.currency = currency
        dataSource.clearRates()
        interactor.fetchRates(currency.code, dataSource.coinCodes)
        view?.reload()
    }

    override fun didUpdateBalance(wallet: Wallet, balance: BigDecimal) {
        val position = dataSource.getPosition(wallet)
        dataSource.setBalance(position, balance)
        updateByPosition(position)
        view?.updateHeader()
    }

    override fun didUpdateState(wallet: Wallet, state: AdapterState) {
        val position = dataSource.getPosition(wallet)
        dataSource.setState(position, state)
        sortCoins()
        updateByPosition(position)
        view?.updateHeader()
    }

    override fun didUpdateRate(rate: Rate) {
        val positions = dataSource.getPositionsByCoinCode(rate.coinCode)
        positions.forEach {
            dataSource.setRate(it, rate)
            updateByPosition(it)
        }
        view?.updateHeader()
        dataSource.sortBy(interactor.getSortingType())
        view?.reload()
    }

    override fun onReceiveRateStats(data: Pair<RateStatsManager.StatsKey, RateStatData>) {
        val (key, rate) = data
        val rateStats = rate.stats[ChartType.DAILY.name] ?: return
        val positions = dataSource.getPositionsByCoinCode(key.coinCode)

        positions.forEach { position ->
            dataSource.setChartData(position, rateStats)
            view?.updateItem(position)
        }
    }

    override fun onFailFetchChartStats(coinCode: String) {
        dataSource.getPositionsByCoinCode(coinCode).forEach { position ->
            dataSource.setChartStatsFetching(position, false)
            view?.updateItem(position)
        }
    }

    override fun onFetchingChartStats(coinCode: String) {
        dataSource.getPositionsByCoinCode(coinCode).forEach { position ->
            dataSource.setChartStatsFetching(position, true)
        }
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    override fun openBackup() {
        accountToBackup?.let { account ->
            val accountType = predefinedAccountTypeManager.allTypes.first { it.supports(account.type) }
            router.openBackup(account, accountType.coinCodes)
            accountToBackup = null
        }
    }

    override fun openChart(position: Int) {
        val wallet = dataSource.getItem(position).wallet
        router.openChart(wallet.coin)
    }

    private fun sortCoins() {
        val syncedCount = dataSource.items.filter { it.state == AdapterState.Synced }.size
        if (dataSource.items.size == syncedCount) {
            dataSource.sortBy(interactor.getSortingType())
        }
    }

    private fun updateViewItems() {
        dataSource.getUpdatedPositions().forEach {
            view?.updateItem(it)
        }
        dataSource.clearUpdatedPositions()
    }

    private fun updateByPosition(position: Int) {
        dataSource.addUpdatedPosition(position)
        flushSubject.onNext(Unit)
    }
}
