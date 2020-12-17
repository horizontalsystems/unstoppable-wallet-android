package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ICoinRecordStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class CoinManager(
        private val appConfigProvider: IAppConfigProvider,
        private val coinRecordStorage: ICoinRecordStorage
) : ICoinManager {

    private val coinAddedSubject = PublishSubject.create<Coin>()

    override val coinAddedObservable: Flowable<Coin>
        get() = coinAddedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val coins: List<Coin>
        get() = coinRecordStorage.coins + appConfigProvider.featuredCoins + appConfigProvider.otherCoins

    override val featuredCoins: List<Coin>
        get() = appConfigProvider.featuredCoins

    override fun save(coin: Coin) {
        if (coinRecordStorage.save(coin)) {
            coinAddedSubject.onNext(coin)
        }
    }
}
