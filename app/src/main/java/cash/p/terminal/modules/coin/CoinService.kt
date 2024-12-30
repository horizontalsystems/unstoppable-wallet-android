package cash.p.terminal.modules.coin

import cash.p.terminal.wallet.Clearable
import cash.p.terminal.core.managers.MarketFavoritesManager
import cash.p.terminal.wallet.entities.FullCoin
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CoinService(
    val fullCoin: FullCoin,
    private val marketFavoritesManager: MarketFavoritesManager,
) : Clearable {

    private val _isFavorite = BehaviorSubject.create<Boolean>()
    val isFavorite: Observable<Boolean>
        get() = _isFavorite

    private val disposables = CompositeDisposable()

    init {
        emitIsFavorite()
    }

    override fun clear() {
        disposables.clear()
    }

    fun favorite() {
        marketFavoritesManager.add(fullCoin.coin.uid)

        emitIsFavorite()
    }

    fun unfavorite() {
        marketFavoritesManager.remove(fullCoin.coin.uid)

        emitIsFavorite()
    }

    private fun emitIsFavorite() {
        _isFavorite.onNext(marketFavoritesManager.isCoinInFavorites(fullCoin.coin.uid))
    }
}
