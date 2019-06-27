package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IEthereumKitManager
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AdapterManager(
        private val walletManager: WalletManager,
        private val authManager: AuthManager,
        private val adapterFactory: AdapterFactory,
        private val ethereumKitManager: IEthereumKitManager)
    : IAdapterManager, HandlerThread("A") {

    private val handler: Handler
    private val disposables = CompositeDisposable()

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

        disposables.add(authManager.authDataSignal
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    initAdapters()
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

    override fun initAdapters() {
        handler.post {
            val oldAdapters = adapters.toMutableList()

            adapters = walletManager.wallets.mapNotNull { wallet ->
                var adapter = adapters.find { it.wallet == wallet }
                if (adapter == null) {
                    // TODO create adapter
                    //adapter = adapterFactory.adapterForCoin(wallet, authData)
                    //adapter?.start()
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
