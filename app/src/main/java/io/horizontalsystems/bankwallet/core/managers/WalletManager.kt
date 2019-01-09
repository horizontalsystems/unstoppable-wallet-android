package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.factories.WalletFactory
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletManager(coinManager: CoinManager, wordsManager: WordsManager, private val walletFactory: WalletFactory) : IWalletManager, HandlerThread("A") {

    private val handler: Handler
    private val compositeDisposable = CompositeDisposable()

    init {
        start()
        handler = Handler(looper)

        handler.post {
            compositeDisposable.add(
                    Flowable.combineLatest(
                            coinManager.coinsObservable,
                            wordsManager.authDataObservable,
                            BiFunction<List<Coin>, AuthData, Pair<List<Coin>, AuthData>> { coins, words -> Pair(coins, words) })
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .subscribe { (coins, authData) ->
                                handle(coins, authData)
                            })
        }
    }

    override var wallets: List<Wallet> = listOf()
    override val walletsUpdatedSignal = PublishSubject.create<Unit>()

    override fun refreshWallets() {
        handler.post {
            wallets.forEach { it.adapter.refresh() }
        }
    }

    override fun clearWallets() {
        TODO("not implemented")
    }

    private fun handle(coins: List<Coin>, authData: AuthData) {
        handler.post {
            wallets = coins.mapNotNull { coin ->
                wallets.find { it.coinCode == coin.code }
                        ?: walletFactory.createWallet(coin, authData)
            }

            walletsUpdatedSignal.onNext(Unit)
        }
    }

}
