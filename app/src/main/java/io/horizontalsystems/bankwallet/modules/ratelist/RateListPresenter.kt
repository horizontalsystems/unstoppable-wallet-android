package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class RateListPresenter(
        val view: RateListView,
        private val interactor: RateListModule.IInteractor,
        private val dataSource: RateListModule.DataSource) : ViewModel(), RateListModule.IViewDelegate, RateListModule.IInteractorDelegate {

    private val reloadViewSubject = PublishSubject.create<Unit>()
    private var disposables = CompositeDisposable()
    private var rateDate: Long = 0

    override val itemsCount: Int
        get() {
            return dataSource.items.size
        }

    override fun viewDidLoad() {
        dataSource.setViewItems(interactor.coins)

        view.showCurrentDate(interactor.currentDate)

        interactor.initRateList()

        interactor.fetchRates(dataSource.coinCodes, interactor.currency.code)
        interactor.getRateStats(dataSource.coinCodes, interactor.currency.code)

        reloadViewSubject
                .throttleLast(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { view.reload() }
                .subscribe()?.let { disposables.add(it) }

        postViewReload()
    }

    override fun willEnterForeground() {
        interactor.getRateStats(dataSource.coinCodes, interactor.currency.code)
    }

    @Synchronized
    override fun didUpdateRate(rate: Rate) {
        dataSource.setRate(rate, interactor.currency)
        updateTimeAgo(rate.timestamp)
        postViewReload()
    }

    private fun updateTimeAgo(timestamp: Long) {
        if (rateDate < timestamp) {
            rateDate = timestamp
            val secondsAgo = DateHelper.getSecondsAgo(timestamp * 1000)
            view.setTimeAgo(secondsAgo)
        }
    }

    override fun onReceive(statsData: StatsData) {
        dataSource.setChartData(statsData)
        postViewReload()
    }

    override fun onFailStats(coinCode: String) {
        dataSource.setStatsFailed(coinCode)
        postViewReload()
    }

    override fun getViewItem(position: Int): RateViewItem {
        return dataSource.items[position]
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
        interactor.clear()
    }

    private fun postViewReload() {
        reloadViewSubject.onNext(Unit)
    }
}
