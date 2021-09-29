package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.MarketCoin
import io.horizontalsystems.marketkit.models.Platform
import io.reactivex.subjects.PublishSubject

class CoinPlatformsService {
    val approvePlatformsObservable = PublishSubject.create<CoinWithPlatforms>()
    val rejectApprovePlatformsObservable = PublishSubject.create<Coin>()
    val requestObservable = PublishSubject.create<Request>()

    fun approvePlatforms(marketCoin: MarketCoin, currentPlatforms: List<Platform> = listOf()) {
        if (marketCoin.platforms.size == 1) {
            approvePlatformsObservable.onNext(CoinWithPlatforms(marketCoin.coin, marketCoin.platforms))
        } else {
            requestObservable.onNext(Request(marketCoin, currentPlatforms))
        }
    }

    fun select(platforms: List<Platform>, coin: Coin) {
        approvePlatformsObservable.onNext(CoinWithPlatforms(coin, platforms))
    }

    fun cancel(coin: Coin) {
        rejectApprovePlatformsObservable.onNext(coin)
    }

    data class CoinWithPlatforms(
        val coin: Coin,
        val platforms: List<Platform> = listOf()
    )

    data class Request(
        val marketCoin: MarketCoin,
        val currentPlatforms: List<Platform>
    )
}
