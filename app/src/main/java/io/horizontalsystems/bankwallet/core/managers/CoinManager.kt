package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.coinkit.CoinKit
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class CoinManager(
        private val coinKit: CoinKit
) : ICoinManager {

    private val coinAddedSubject = PublishSubject.create<Coin>()

    override val coinAddedObservable: Flowable<Coin>
        get() = coinAddedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val coins: List<Coin>
        get() = coinKit.getCoins()

    override val featuredCoins: List<Coin>
        get() = coinKit.getDefaultCoins().take(8)

    override fun getCoin(coinType: CoinType): Coin?{
        return coinKit.getCoin(coinType)
    }

    override fun save(coin: Coin) {
        coinKit.saveCoin(coin)
        coinAddedSubject.onNext(coin)
    }
}
