package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.subjects.PublishSubject

class WalletManager(private val adapterFactory: AdapterFactory) : IWalletManager, HandlerThread("A") {

    private val handler: Handler

    init {
        start()
        handler = Handler(looper)
    }

    override var wallets: List<Wallet> = listOf()
    override val walletsSubject = PublishSubject.create<List<Wallet>>()

    override fun initWallets(words: List<String>, coins: List<Coin>, newWallet: Boolean) {
        handler.post {
            val newWallets = mutableListOf<Wallet>()

            wallets = coins.mapNotNull { coin ->
                var wallet = wallets.firstOrNull { it.coinCode == coin.code }

                if (wallet != null) {
                    wallet
                } else {
                    val adapter = adapterFactory.adapterForCoin(coin.type, words, newWallet)

                    if (adapter == null) {
                        null
                    } else {
                        wallet = Wallet(coin.title, coin.code, adapter)

                        newWallets.add(wallet)
                        wallet
                    }
                }
            }

            walletsSubject.onNext(wallets)

            newWallets.forEach { it.adapter.start() }
        }
    }

    override fun refreshWallets() {
        handler.post {
            wallets.forEach { it.adapter.refresh() }
        }
    }

    override fun clearWallets() {
        handler.post {
            wallets.forEach { it.adapter.clear() }
            wallets = listOf()
        }
    }
}
