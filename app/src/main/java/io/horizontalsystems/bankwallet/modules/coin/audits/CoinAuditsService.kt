package io.horizontalsystems.bankwallet.modules.coin.audits

import io.horizontalsystems.bankwallet.core.logoUrl
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsModule.AuditorItem
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.Auditor
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class CoinAuditsService(
    private val addresses: List<String>,
    private val marketKit: MarketKit
) {
    private var disposable: Disposable? = null

    private val stateSubject = BehaviorSubject.create<DataState<List<AuditorItem>>>()
    val stateObservable: Observable<DataState<List<AuditorItem>>>
        get() = stateSubject

    private fun fetch() {
        disposable?.dispose()

        marketKit.auditReportsSingle(addresses)
            .doOnSubscribe { stateSubject.onNext(DataState.Loading) }
            .subscribeIO({ auditors ->
                stateSubject.onNext(DataState.Success(auditorItems(auditors)))
            }, { error ->
                stateSubject.onNext(DataState.Error(error))
            }).let { disposable = it }
    }

    private fun auditorItems(auditors: List<Auditor>): List<AuditorItem> {
        val auditorItems = auditors.map { auditor ->
            val sortedReports = auditor.reports.sortedByDescendingNullLast { it.date }
            AuditorItem(auditor.name, auditor.logoUrl, sortedReports, sortedReports.firstOrNull()?.date)
        }
        return auditorItems.sortedByDescendingNullLast { it.latestDate }
    }

    fun start() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        disposable?.dispose()
    }
}
