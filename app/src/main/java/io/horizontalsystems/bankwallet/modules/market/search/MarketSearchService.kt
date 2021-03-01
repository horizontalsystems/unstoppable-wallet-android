package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.reactivex.subjects.BehaviorSubject

class MarketSearchService(private val xRateManager: IRateManager) : Clearable {

    var query: String = ""
        set(value) {
            field = value

            fetch()
        }

    sealed class State {
        object Idle: State()
        object Loading: State()
        object Error: State()
        class Success(val items: List<Any>): State()
    }

    val stateAsync: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Idle)

    private fun fetch() {
        val queryTrimmed = query.trim()

        if (queryTrimmed.count() < 2) {
            stateAsync.onNext(State.Idle)
        } else {
            stateAsync.onNext(State.Loading)

//            xRateManager.getCoins(queryTrimmed)

            stateAsync.onNext(State.Success(listOf()))
        }
    }

    override fun clear() = Unit
}