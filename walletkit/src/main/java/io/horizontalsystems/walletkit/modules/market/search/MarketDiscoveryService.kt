package io.horizontalsystems.walletkit.modules.market.search

import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.isSynthetic
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MarketDiscoveryService(
    private val marketKit: MarketKitWrapper,
    private val localStorage: ILocalStorage,
) {
    private var recentCoins: List<FullCoin> = listOf()
    private var popularCoins: List<FullCoin> = listOf()

    private val _stateFlow = MutableStateFlow(
        State(
            recent = recentCoins,
            popular = popularCoins,
        )
    )
    val stateFlow: StateFlow<State>
        get() = _stateFlow.asStateFlow()

    fun start() {
        recentCoins = marketKit
            .fullCoins(localStorage.marketSearchRecentCoinUids)
            .sortedBy {
                localStorage.marketSearchRecentCoinUids.indexOf(it.coin.uid)
            }
        // Safety net: hide synthetic coins in case they ever get a market-cap rank.
        // Recent list is intentionally left untouched.
        popularCoins = marketKit.fullCoins("").filter { !it.coin.isSynthetic }

        emitState()
    }

    private fun emitState() {
        _stateFlow.update {
            State(
                recent = recentCoins,
                popular = popularCoins
            )
        }
    }

    fun addCoinToRecent(coin: Coin) {
        localStorage.marketSearchRecentCoinUids =
            (listOf(coin.uid) + localStorage.marketSearchRecentCoinUids).distinct().take(5)
    }

    data class State(
        val recent: List<FullCoin>,
        val popular: List<FullCoin>,
    )
}
