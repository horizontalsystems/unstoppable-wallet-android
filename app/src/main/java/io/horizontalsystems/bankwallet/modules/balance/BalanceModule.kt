package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import java.math.BigDecimal

object BalanceModule {

    interface IView {
        fun didRefresh()
        fun reload()
        fun updateItem(position: Int)
        fun updateHeader()
        fun setSortingOn(isOn: Boolean)
        fun showBackupAlert()
    }

    interface IViewDelegate {
        val itemsCount: Int

        fun viewDidLoad()
        fun getViewItem(position: Int): BalanceViewItem
        fun getHeaderViewItem(): BalanceHeaderViewItem
        fun refresh()
        fun onReceive(position: Int)
        fun onPay(position: Int)
        fun openManageCoins()
        fun onClear()
        fun onSortClick()
        fun onEnableChart(enabled: Boolean)
        fun onSortTypeChanged(sortType: BalanceSortType)
        fun openBackup()
        fun openChart(position: Int)
    }

    interface IInteractor {
        fun refresh()
        fun initWallets()
        fun fetchRates(currencyCode: String, coinCodes: List<CoinCode>)
        fun fetchRateStats(currencyCode: String, coinCode: CoinCode)
        fun getSortingType(): BalanceSortType
        fun clear()
        fun saveSortingType(sortType: BalanceSortType)
        fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
    }

    interface IInteractorDelegate {
        fun didUpdateWallets(wallets: List<Wallet>)
        fun didUpdateCurrency(currency: Currency)
        fun didUpdateBalance(wallet: Wallet, balance: BigDecimal)
        fun didUpdateState(wallet: Wallet, state: AdapterState)
        fun didUpdateRate(rate: Rate)
        fun onReceiveRateStats(coinCode: CoinCode, data: StatsData)
        fun onFailFetchChartStats(coinCode: String)
        fun didRefresh()
    }

    interface IRouter {
        fun openReceiveDialog(wallet: Wallet)
        fun openSendDialog(wallet: Wallet)
        fun openManageCoins()
        fun openSortTypeDialog(sortingType: BalanceSortType)
        fun openBackup(account: Account, coinCodesStringRes: Int)
        fun openChart(coin: Coin)
    }

    class DataSource(var currency: Currency) {
        private var updatedPositions = mutableListOf<Int>()

        val count
            get() = items.count()

        val coinCodes: List<CoinCode>
            get() = items.map { it.wallet.coin.code }.distinct()

        var items = listOf<BalanceItem>()
        var sortType: BalanceSortType = BalanceSortType.Name
            set(value) {
                field = value
                items = sorted(items)
            }

        var chartEnabled = false

        @Synchronized
        fun addUpdatedPosition(position: Int) {
            updatedPositions.add(position)
        }

        @Synchronized
        fun clearUpdatedPositions() {
            updatedPositions.clear()
        }

        fun set(items: List<BalanceItem>) {
            this.items = sorted(items)
            clearUpdatedPositions()
        }

        @Synchronized
        fun getUpdatedPositions(): List<Int> = updatedPositions.distinct()

        fun getItem(position: Int): BalanceItem = items[position]

        fun getPosition(wallet: Wallet): Int {
            return items.indexOfFirst { it.wallet == wallet }
        }

        fun getPositionsByCoinCode(coinCode: String): List<Int> {
            return items.mapIndexedNotNull { index, balanceItem ->
                if (balanceItem.wallet.coin.code == coinCode) {
                    index
                } else {
                    null
                }
            }
        }

        fun setBalance(position: Int, balance: BigDecimal) {
            items[position].balance = balance
        }

        fun setState(position: Int, state: AdapterState) {
            items[position].state = state

            if (items.all { it.state == AdapterState.Synced }) {
                items = sorted(items)
            }
        }

        fun setRate(position: Int, rate: Rate) {
            items[position].rate = rate
            items = sorted(items)
        }

        fun setChartData(position: Int, chartData: ChartData, chartDiff: BigDecimal) {
            items[position].chartDiff = chartDiff
            items[position].chartData = chartData
        }

        fun clearRates() {
            items.forEach { it.rate = null }
        }

        private fun sorted(items: List<BalanceItem>) = when (sortType) {
            BalanceSortType.Value -> {
                items.sortedByDescending { it.fiatValue }
            }
            BalanceSortType.Name -> {
                items.sortedBy { it.wallet.coin.title }
            }
        }
    }

    data class BalanceItem(
            val wallet: Wallet,
            var balance: BigDecimal,
            var state: AdapterState,
            var rate: Rate? = null) {

        var chartDiff: BigDecimal = BigDecimal.ZERO
        var chartData: ChartData? = null
        val fiatValue: BigDecimal?
            get() = rate?.let { balance.times(it.value) }
    }

    fun init(view: BalanceViewModel, router: IRouter) {
        val currencyManager = App.currencyManager
        val interactor = BalanceInteractor(App.walletManager, App.adapterManager, App.rateStorage, App.rateStatsManager, currencyManager, App.localStorage)
        val presenter = BalancePresenter(interactor, router, DataSource(currencyManager.baseCurrency), App.predefinedAccountTypeManager, BalanceViewItemFactory())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }
}
