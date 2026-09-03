package io.horizontalsystems.walletkit.modules.coin

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.managers.MarketFavoritesManager
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CoinService(
    val fullCoin: FullCoin,
    private val marketFavoritesManager: MarketFavoritesManager,
) : Clearable {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean>
        get() = _isFavorite

    init {
        emitIsFavorite()
    }

    override fun clear() = Unit

    fun favorite() {
        marketFavoritesManager.add(fullCoin.coin.uid)

        emitIsFavorite()
    }

    fun unfavorite() {
        marketFavoritesManager.remove(fullCoin.coin.uid)

        emitIsFavorite()
    }

    private fun emitIsFavorite() {
        _isFavorite.value = marketFavoritesManager.isCoinInFavorites(fullCoin.coin.uid)
    }
}
