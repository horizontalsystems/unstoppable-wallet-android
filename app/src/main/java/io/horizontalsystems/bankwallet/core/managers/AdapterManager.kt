package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AdapterManager(
        private val walletManager: IWalletManager,
        private val adapterFactory: AdapterFactory,
        private val ethereumKitManager: IEthereumKitManager,
        private val eosKitManager: IEosKitManager,
        private val binanceKitManager: BinanceKitManager)
    : IAdapterManager, HandlerThread("A") {

    private val handler: Handler
    private val disposables = CompositeDisposable()

    init {
        start()
        handler = Handler(looper)

        disposables.add(walletManager.walletsObservable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    initAdapters(it)
                }
        )
    }

    override val adapters: List<IAdapter> get() = TODO("Deprecated")

    override val adaptersUpdatedSignal = PublishSubject.create<Unit>()

    override fun preloadAdapters() {
        initAdapters(walletManager.wallets)
    }

    private val adaptersMap = mutableMapOf<Wallet, IAdapter>()

    override fun refresh() {
        handler.post {
            adaptersMap.values.forEach { it.refresh() }
        }

        ethereumKitManager.ethereumKit?.refresh()
        eosKitManager.eosKit?.refresh()
        binanceKitManager.binanceKit?.refresh()
    }

    override fun initAdapters(wallets: List<Wallet>) {
        handler.post {
            val disabledWallets = adaptersMap.keys.subtract(wallets)

            wallets.forEach { wallet ->
                if (!adaptersMap.containsKey(wallet)) {
                    adapterFactory.adapterForCoin(wallet)?.let { adapter ->
                        adapter.start()
                        adaptersMap[wallet] = adapter
                    }
                }
            }

            adaptersUpdatedSignal.onNext(Unit)

            disabledWallets.forEach { wallet ->
                adaptersMap.remove(wallet)?.let { disabledAdapter ->
                    disabledAdapter.stop()
                    adapterFactory.unlinkAdapter(disabledAdapter)
                }
            }
        }
    }

    override fun stopKits() {
        handler.post {
            adaptersMap.values.forEach {
                it.stop()
                adapterFactory.unlinkAdapter(it)
            }
            adaptersMap.clear()
            adaptersUpdatedSignal.onNext(Unit)
        }
    }
}
