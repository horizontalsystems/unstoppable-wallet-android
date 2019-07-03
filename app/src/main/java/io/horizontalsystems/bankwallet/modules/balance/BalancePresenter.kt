package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class BalancePresenter(
        private var interactor: BalanceModule.IInteractor,
        private val router: BalanceModule.IRouter,
        private val dataSource: BalanceModule.BalanceItemDataSource,
        private val factory: BalanceViewItemFactory) : BalanceModule.IViewDelegate, BalanceModule.IInteractorDelegate {

    var view: BalanceModule.IView? = null
    private val disposables = CompositeDisposable()
    private var flushSubject = PublishSubject.create<Unit>()
    private val showSortingButtonThreshold = 5

    //
    // BalanceModule.IViewDelegate
    //
    override val itemsCount: Int
        get() = dataSource.count

    override fun viewDidLoad() {
        interactor.initAdapters()

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
                    dataSource.items.forEach { item ->
                        item.rate?.let { rate ->
                            didUpdateRate(rate)
                        }
                    }
                }?.let { disposables.add(it) }
    }

    override fun didEnabledCoinsCountUpdated(size: Int) {
        view?.enabledCoinsCount(size)
    }

    override fun getViewItem(position: Int) =
            factory.createViewItem(dataSource.getItem(position), dataSource.currency)

    override fun getHeaderViewItem() =
            factory.createHeaderViewItem(dataSource.items, dataSource.currency)

    override fun refresh() {
        interactor.refresh()
    }

    override fun onReceive(position: Int) {
        router.openReceiveDialog(dataSource.getItem(position).coin.code)
    }

    override fun onPay(position: Int) {
        router.openSendDialog(dataSource.getItem(position).coin.code)
    }

    override fun onClear() {
        interactor.clear()
        disposables.clear()
    }

    //
    // BalanceModule.IInteractorDelegate
    //
    override fun didUpdateAdapters(adapters: List<IAdapter>) {
        val items = adapters.map { BalanceModule.BalanceItem(it.coin, it.balance, it.state) }
        dataSource.set(items)
        dataSource.currency?.let {
            interactor.fetchRates(it.code, dataSource.coinCodes)
        }

        view?.setSortingOn(items.size > showSortingButtonThreshold)
        view?.reload()
    }

    override fun didUpdateCurrency(currency: Currency) {
        dataSource.currency = currency
        dataSource.clearRates()
        interactor.fetchRates(currency.code, dataSource.coinCodes)
        view?.reload()
    }

    override fun didUpdateBalance(coinCode: CoinCode, balance: BigDecimal) {
        val position = dataSource.getPosition(coinCode)
        dataSource.setBalance(position, balance)
        updateByPosition(position)
        view?.updateHeader()
    }

    override fun didUpdateState(coinCode: String, state: AdapterState) {
        val position = dataSource.getPosition(coinCode)
        dataSource.setState(position, state)
        updateByPosition(position)
        view?.updateHeader()
    }

    override fun didUpdateRate(rate: Rate) {
        val position = dataSource.getPosition(rate.coinCode)
        dataSource.setRate(position, rate)
        updateByPosition(position)
        view?.updateHeader()
    }

    override fun didRefresh() {
        view?.didRefresh()
    }

    override fun openManageCoins() {
        router.openManageCoins()
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

    override fun onSortClick() {
        router.openSortTypeDialog()
    }

    override fun onSortTypeChanged(sortType: BalanceSortType) {
        dataSource.sortBy(sortType)
        view?.reload()
    }
}
