package io.horizontalsystems.bankwallet.modules.market.category

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinCategory
import io.horizontalsystems.marketkit.models.MarketInfo
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject

class MarketTopCoinsService(
    coinCategoryUid: String,
    private val marketKit: MarketKit,
    val baseCurrency: Currency
) : Clearable {

    sealed class State {
        object Loading : State()
        object Loaded : State()
        data class Error(val error: Throwable) : State()
    }

    val coinCategory: CoinCategory? = marketKit.coinCategory(coinCategoryUid)

    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)
    var marketInfoItems: List<MarketInfo> = listOf()
    private var disposable: Disposable? = null

    fun fetchCoinList(top: Int, limit: Int?, order: MarketInfo.Order) {
        disposable?.dispose()
        disposable = marketKit.marketInfosSingle(top, limit, order)
            .subscribeIO({
                marketInfoItems = it
                stateObservable.onNext(State.Loaded)
            }, {
                stateObservable.onNext(State.Error(it))
            })
    }

    override fun clear() {
        disposable?.dispose()
    }
}
