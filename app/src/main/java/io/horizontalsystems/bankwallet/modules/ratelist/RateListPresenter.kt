package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.StatsData
import io.horizontalsystems.bankwallet.entities.Rate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class RateListPresenter(
        val view: RateListView,
        private val interactor: RateListModule.IInteractor,
        private val dataSource: RateListModule.DataSource): ViewModel(), RateListModule.IViewDelegate, RateListModule.IInteractorDelegate  {

    private val reloadViewSubject = PublishSubject.create<Unit>()
    private var disposables = CompositeDisposable()

    override val itemsCount: Int
        get() {
            return dataSource.items.size
        }

    override fun viewDidLoad() {
        view.showCurrentDate(interactor.currentDate)

        interactor.initRateList()

        interactor.fetchRates(dataSource.coinCodes, dataSource.baseCurrency.code)
        interactor.getRateStats(dataSource.coinCodes, dataSource.baseCurrency.code)

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
        interactor.getRateStats(dataSource.coinCodes, dataSource.baseCurrency.code)
    }

    @Synchronized
    override fun didUpdateRate(rate: Rate) {
        dataSource.setRate(rate)
        postViewReload()
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
