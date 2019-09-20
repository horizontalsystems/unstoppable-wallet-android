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

    @Synchronized
    override fun didUpdateRate(rate: Rate) {
        dataSource.getPositionsByCoinCode(rate.coinCode).forEach { position ->
            dataSource.setRate(position, rate)
        }
        postViewReload()
    }

    override fun onReceiveRateStats(statsData: StatsData) {
        val positions = dataSource.getPositionsByCoinCode(statsData.coinCode)
        val chartDiff = statsData.diff[dataSource.chartType]

        positions.forEach { position ->
            dataSource.setChartData(position, chartDiff)
        }

        postViewReload()
    }

    override fun onFailFetchChartStats(coinCode: String) {
        dataSource.getPositionsByCoinCode(coinCode).forEach { position ->
            dataSource.setLoadingStatusAsFailed(position)
        }
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
