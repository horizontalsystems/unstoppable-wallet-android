package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MarketSearchService(private val marketKit: MarketKitWrapper) {
    private var query: String = ""
    private var results: List<FullCoin> = listOf()

    private val _stateFlow = MutableStateFlow(
        State(
            results = results,
            query = query
        )
    )
    val stateFlow: StateFlow<State>
        get() = _stateFlow.asStateFlow()

    private fun emitState() {
        _stateFlow.update {
            State(
                results = results,
                query = query
            )
        }
    }

    fun setQuery(query: String) {
        this.query = query

        refreshResults()
        emitState()
    }

    private fun refreshResults() {
        results = if (query.isBlank()) {
            listOf()
        } else {
            marketKit.fullCoins(query)
        }
    }

    data class State(val results: List<FullCoin>, val query: String)
}
