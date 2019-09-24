package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import java.math.BigDecimal

object BalanceModule {

    interface IView {
        fun didRefresh()
        fun reload()
        fun updateItem(position: Int)
        fun updateHeader()
        fun setSortingOn(isOn: Boolean)
        fun showBackupAlert(coin: Coin, predefinedAccountType: IPredefinedAccountType)
        fun setStatsButton(state: StatsButtonState)
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
        fun onChartClick()
        fun onSortTypeChanged(sortType: BalanceSortType)
        fun openBackup()
        fun openChart(position: Int)
    }

    interface IInteractor {
        fun refresh()
        fun initWallets()
        fun fetchRates(currencyCode: String, coinCodes: List<CoinCode>)
        fun getSortingType(): BalanceSortType
        fun clear()
        fun saveSortingType(sortType: BalanceSortType)
        fun getBalanceAdapterForWallet(wallet: Wallet): IBalanceAdapter?
        fun syncStats(coinCode: String, currencyCode: String)
        fun predefinedAccountType(wallet: Wallet): IPredefinedAccountType?
    }

    interface IInteractorDelegate {
        fun didUpdateWallets(wallets: List<Wallet>)
        fun didUpdateCurrency(currency: Currency)
        fun didUpdateBalance(wallet: Wallet, balance: BigDecimal)
        fun didUpdateState(wallet: Wallet, state: AdapterState)
        fun didUpdateRate(rate: Rate)
        fun onReceiveRateStats(data: StatsData)
        fun onFailFetchChartStats(coinCode: String)
        fun didRefresh()
        fun willEnterForeground()
    }

    interface IRouter {
        fun openReceiveDialog(wallet: Wallet)
        fun openSendDialog(wallet: Wallet)
        fun openManageCoins()
        fun openSortTypeDialog(sortingType: BalanceSortType)
        fun openBackup(account: Account, coinCodesStringRes: Int)
        fun openChart(coin: Coin)
    }

    interface IBalanceSorter {
        fun sort(items: List<BalanceItem>, sortType: BalanceSortType): List<BalanceItem>
    }

    enum class StatsButtonState {
        NORMAL, HIDDEN, SELECTED
    }

    class DataSource(var currency: Currency, private val sorter: IBalanceSorter) {
        private var updatedPositions = mutableListOf<Int>()

        val count
            get() = items.count()

        val coinCodes: List<CoinCode>
            get() = items.map { it.wallet.coin.code }.distinct()

        var items = listOf<BalanceItem>()
        var sortType: BalanceSortType = BalanceSortType.Name
            set(value) {
                field = value
                items = sorter.sort(items, sortType)
            }

        var statsButtonState: StatsButtonState = StatsButtonState.HIDDEN

        @Synchronized
        fun addUpdatedPosition(position: Int) {
            updatedPositions.add(position)
        }

        @Synchronized
        fun clearUpdatedPositions() {
            updatedPositions.clear()
        }

        fun set(items: List<BalanceItem>) {
            this.items = sorter.sort(items, sortType)
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
                items = sorter.sort(items, sortType)
            }
        }

        fun setRate(position: Int, rate: Rate) {
            items[position].rate = rate
            items = sorter.sort(items, sortType)
        }

        fun setChartData(position: Int, data: BalanceChartData) {
            items[position].chartData = data
            items = sorter.sort(items, sortType)
        }

        fun clearRates() {
            items.forEach { it.rate = null }
        }

    }

    data class BalanceItem(
            val wallet: Wallet,
            var balance: BigDecimal,
            var state: AdapterState,
            var rate: Rate? = null) {

        var chartData: BalanceChartData? = null
        val fiatValue: BigDecimal?
            get() = rate?.let { balance.times(it.value) }
    }

    fun init(view: BalanceViewModel, router: IRouter) {
        val currencyManager = App.currencyManager
        val interactor = BalanceInteractor(App.walletManager, App.adapterManager, App.rateStatsManager, currencyManager, App.backgroundManager, App.rateStorage, App.localStorage, App.rateManager, App.predefinedAccountTypeManager)
        val presenter = BalancePresenter(interactor, router, DataSource(currencyManager.baseCurrency, BalanceSorter()), App.predefinedAccountTypeManager, BalanceViewItemFactory())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }
}
