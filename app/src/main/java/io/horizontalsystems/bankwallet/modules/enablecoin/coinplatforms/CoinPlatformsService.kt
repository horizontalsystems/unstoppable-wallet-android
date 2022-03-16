package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import io.horizontalsystems.bankwallet.entities.supportedPlatforms
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Platform
import io.reactivex.subjects.PublishSubject

class CoinPlatformsService {
    val approvePlatformsObservable = PublishSubject.create<CoinWithPlatforms>()
    val rejectApprovePlatformsObservable = PublishSubject.create<FullCoin>()
    val requestObservable = PublishSubject.create<Request>()

    fun approvePlatforms(fullCoin: FullCoin, currentPlatforms: List<Platform> = listOf()) {
        val supportedPlatforms = fullCoin.supportedPlatforms
        if (supportedPlatforms.size == 1) {
            approvePlatformsObservable.onNext(CoinWithPlatforms(fullCoin.coin, supportedPlatforms))
        } else {
            requestObservable.onNext(Request(fullCoin, currentPlatforms))
        }
    }

    fun select(platforms: List<Platform>, coin: Coin) {
        approvePlatformsObservable.onNext(CoinWithPlatforms(coin, platforms))
    }

    fun cancel(fullCoin: FullCoin) {
        rejectApprovePlatformsObservable.onNext(fullCoin)
    }

    data class CoinWithPlatforms(
        val coin: Coin,
        val platforms: List<Platform> = listOf()
    )

    data class Request(
        val fullCoin: FullCoin,
        val currentPlatforms: List<Platform>
    )
}
