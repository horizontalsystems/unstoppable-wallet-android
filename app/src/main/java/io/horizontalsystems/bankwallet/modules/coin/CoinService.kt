package io.horizontalsystems.bankwallet.modules.coin

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.INotificationManager
import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class CoinService(
    private val coinUid: String,
    private val coinManager: ICoinManager,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val priceAlertManager: IPriceAlertManager,
    private val notificationManager: INotificationManager
) : Clearable {
    val fullCoin = coinManager.getFullCoin(coinUid)!!

    private val _isFavorite = BehaviorSubject.create<Boolean>()
    val isFavorite: Observable<Boolean>
        get() = _isFavorite

    val alertNotificationUpdatedObservable: BehaviorSubject<Unit> = BehaviorSubject.createDefault(Unit)
    val hasPriceAlert: Boolean
//    todo: replace coinType with coinUid
        get() = priceAlertManager.hasPriceAlert(fullCoin.platforms.first().coinType)
    val notificationsAreEnabled: Boolean
        get() = notificationManager.enabled


    private val disposables = CompositeDisposable()

    init {
        _isFavorite.onNext(marketFavoritesManager.isCoinInFavorites(fullCoin.coin.uid))

        priceAlertManager.notificationChangedFlowable
            .subscribeOn(Schedulers.io())
            .subscribe {
                alertNotificationUpdatedObservable.onNext(Unit)
            }
            .let {
                disposables.add(it)
            }
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
