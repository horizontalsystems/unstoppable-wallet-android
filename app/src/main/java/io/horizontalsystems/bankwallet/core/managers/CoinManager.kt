package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.coinkit.CoinKit
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class CoinManager(
        private val coinKit: CoinKit,
        private val appConfigProvider: IAppConfigProvider
) : ICoinManager {

    private val coinAddedSubject = PublishSubject.create<Coin>()

    override val coinAddedObservable: Flowable<Coin>
        get() = coinAddedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val coins: List<Coin>
        get() = coinKit.getCoins()

    override val groupedCoins: Pair<List<Coin>, List<Coin>>
        get() {
            val featured = mutableListOf<Coin>()
            val coins = coinKit.getCoins().toMutableList()

            appConfigProvider.featuredCoinTypes.forEach { coinType ->
                coins.indexOfFirst { it.type == coinType }.let { index ->
                    if (index != -1) {
                        featured.add(coins[index])
                        coins.removeAt(index)
                    }
                }
            }
            return Pair(featured, coins)
        }

    override fun getCoin(coinType: CoinType): Coin?{
        return coinKit.getCoin(coinType)
    }

    override fun save(coin: Coin) {
        coinKit.saveCoin(coin)
        coinAddedSubject.onNext(coin)
    }
}
