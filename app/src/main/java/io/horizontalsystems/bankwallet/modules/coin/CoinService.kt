package io.horizontalsystems.bankwallet.modules.coin

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CoinService(
    private val coinUid: String,
    private val coinManager: ICoinManager,
    private val marketFavoritesManager: MarketFavoritesManager,
) : Clearable {
    val fullCoin = coinManager.getFullCoin(coinUid)!!

    private val _isFavorite = BehaviorSubject.create<Boolean>()
    val isFavorite: Observable<Boolean>
        get() = _isFavorite

    private val disposables = CompositeDisposable()

    init {
        _isFavorite.onNext(marketFavoritesManager.isCoinInFavorites(fullCoin.coin.uid))

    }

    override fun clear() {
        disposables.clear()
    }

    fun favorite() {
        marketFavoritesManager.add(fullCoin.coin.uid)

        _isFavorite.onNext(marketFavoritesManager.isCoinInFavorites(fullCoin.coin.uid))
    }

    fun unfavorite() {
        marketFavoritesManager.remove(fullCoin.coin.uid)

        _isFavorite.onNext(marketFavoritesManager.isCoinInFavorites(fullCoin.coin.uid))
    }
}
