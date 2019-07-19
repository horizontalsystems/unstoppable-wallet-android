package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AdapterManager(walletManager: IWalletManager, private val adapterFactory: AdapterFactory, private val ethereumKitManager: IEthereumKitManager)
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

    override var adapters: List<IAdapter> = listOf()
    override val adaptersUpdatedSignal = PublishSubject.create<Unit>()

    override fun refresh() {
        handler.post {
            adapters.forEach { it.refresh() }
        }

        ethereumKitManager.ethereumKit?.refresh()
    }

    override fun initAdapters(wallets: List<Wallet>) {
        handler.post {
            val oldAdapters = adapters.toMutableList()

            adapters = wallets.mapNotNull { wallet ->
                var adapter = adapters.find { it.wallet == wallet }
                if (adapter == null) {
                    adapter = adapterFactory.adapterForCoin(wallet)
                    adapter?.start()
                }
                adapter
            }

            adaptersUpdatedSignal.onNext(Unit)

            oldAdapters.forEach { oldAdapter ->
                if (adapters.none { it.wallet == oldAdapter.wallet }) {
                    oldAdapter.stop()
                    adapterFactory.unlinkAdapter(oldAdapter)
                }
            }

            oldAdapters.clear()
        }
    }

    override fun stopKits() {
        handler.post {
            adapters.forEach {
                it.stop()
                adapterFactory.unlinkAdapter(it)
            }
            adapters = listOf()
            adaptersUpdatedSignal.onNext(Unit)
        }
    }
}
