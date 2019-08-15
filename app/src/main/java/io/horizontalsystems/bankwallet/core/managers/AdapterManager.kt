package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
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
    private val adapterCreationSubject = PublishSubject.create<Wallet>()
    private val adaptersReadySubject = PublishSubject.create<Unit>()

    override val adapterCreationObservable: Flowable<Wallet> = adapterCreationSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val adaptersReadyObservable: Flowable<Unit> = adaptersReadySubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        start()
        handler = Handler(looper)

        disposables.add(walletManager.walletsUpdatedSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    initAdapters()
                }
        )
    }

    override fun preloadAdapters() {
        initAdapters()
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

    private fun initAdapters() {
        handler.post {
            val wallets = walletManager.wallets
            val disabledWallets = adaptersMap.keys.subtract(wallets)

            wallets.forEach { wallet ->
                if (!adaptersMap.containsKey(wallet)) {
                    adapterFactory.adapterForCoin(wallet)?.let { adapter ->
                        adaptersMap[wallet] = adapter
                        adapterCreationSubject.onNext(wallet)

                        adapter.start()
                    }
                }
            }

            adaptersReadySubject.onNext(Unit)

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
        }
    }

    override fun getAdapterForWallet(wallet: Wallet): IAdapter? {
        return adaptersMap[wallet]
    }

    override fun getTransactionsAdapterForWallet(wallet: Wallet): ITransactionsAdapter? {
        return adaptersMap[wallet]?.let { it as? ITransactionsAdapter }
    }
}
