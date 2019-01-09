package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.factories.WalletFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletManager(private val coinManager: CoinManager, private val authManager: AuthManager, private val walletFactory: WalletFactory) : IWalletManager, HandlerThread("A") {

    private val handler: Handler
    private val disposables = CompositeDisposable()

    init {
        start()
        handler = Handler(looper)

        initWallets()

        disposables.add(authManager.authDataSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    initWallets()
                }
        )
    }

    override var wallets: List<Wallet> = listOf()
    override val walletsUpdatedSignal = PublishSubject.create<Unit>()

    override fun refreshWallets() {
        handler.post {
            wallets.forEach { it.adapter.refresh() }
        }
    }

    override fun initWallets() {
        authManager.authData?.let { authData ->
            handler.post {
                wallets = coinManager.coins.mapNotNull { coin ->
                    wallets.find { it.coinCode == coin.code }
                            ?: walletFactory.createWallet(coin, authData)
                }

                walletsUpdatedSignal.onNext(Unit)
            }
        }
    }

    override fun clearWallets() {
        handler.post {
            wallets.forEach { it.adapter.clear() }
            wallets = listOf()
            walletsUpdatedSignal.onNext(Unit)
        }
    }
}
