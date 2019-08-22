package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
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
    }

    interface IInteractorDelegate {
        fun didUpdateWallets(wallets: List<Wallet>)
        fun didUpdateCurrency(currency: Currency)
        fun didUpdateBalance(wallet: Wallet, balance: BigDecimal)
        fun didUpdateState(wallet: Wallet, state: AdapterState)
        fun didUpdateRate(rate: Rate)
        fun didRefresh()
    }

    interface IRouter {
        fun openReceiveDialog(wallet: Wallet)
        fun openSendDialog(wallet: Wallet)
        fun openManageCoins()
        fun openSortTypeDialog(sortingType: BalanceSortType)
        fun openBackup(account: Account, coinCodesStringRes: Int)
        fun openChart(coin: Coin, rate: Rate?)
    }

    class BalanceItemDataSource {
        private var updatedPositions = mutableListOf<Int>()

        val count
            get() = items.count()

        var currency: Currency? = null

        val coinCodes: List<CoinCode>
            get() = items.map { it.wallet.coin.code }.distinct()

        private var originalItems = listOf<BalanceItem>()
        var items = listOf<BalanceItem>()
        var balanceSortType: BalanceSortType = BalanceSortType.Name

        @Synchronized
        fun addUpdatedPosition(position: Int) {
            updatedPositions.add(position)
        }

        @Synchronized
        fun clearUpdatedPositions() {
            updatedPositions.clear()
        }

        fun set(items: List<BalanceItem>) {
            clearUpdatedPositions()
            originalItems = items
            sortBy(BalanceSortType.Name)
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
        }

        fun setRate(position: Int, rate: Rate) {
            items[position].rate = rate
        }

        fun clearRates() {
            items.forEach {
                it.rate = null
            }
        }

        fun sortBy(sortType: BalanceSortType) {
            balanceSortType = sortType
            items = when (balanceSortType) {
                BalanceSortType.Value -> {
                    originalItems.sortedByDescending { it.fiatValue }
                }
                BalanceSortType.Name ->{
                    originalItems.sortedBy { it.wallet.coin.title}
                }
            }

        }

    }

    data class BalanceItem(
            val wallet: Wallet,
            var balance: BigDecimal = BigDecimal.ZERO,
            var state: AdapterState = AdapterState.NotSynced,
            var rate: Rate? = null
    ) {
        val fiatValue: BigDecimal?
            get() = rate?.let { balance.times(it.value) }
    }

    fun init(view: BalanceViewModel, router: IRouter) {
        val interactor = BalanceInteractor(App.walletManager, App.adapterManager, App.rateStorage, App.currencyManager, App.localStorage)
        val presenter = BalancePresenter(interactor, router, BalanceItemDataSource(), App.predefinedAccountTypeManager, BalanceViewItemFactory())

        presenter.view = view
        interactor.delegate = presenter
        view.delegate = presenter
    }

}
