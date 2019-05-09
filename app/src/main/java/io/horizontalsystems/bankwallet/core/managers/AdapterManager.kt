package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AdapterManager(private val coinManager: CoinManager, private val authManager: AuthManager, private val adapterFactory: AdapterFactory)
    : IAdapterManager, HandlerThread("A") {

    private val handler: Handler
    private val disposables = CompositeDisposable()

    init {
        start()
        handler = Handler(looper)

        disposables.add(coinManager.coinsUpdatedSignal
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

    override fun refreshAdapters() {
        handler.post {
            adapters.forEach { it.refresh() }
        }
    }

    override fun initAdapters() {
        handler.post {
            authManager.authData?.let { authData ->
                val oldAdapters = adapters.toMutableList()

                adapters = coinManager.coins.mapNotNull { coin ->
                    var adapter = adapters.find { it.coin.code == coin.code }
                    if (adapter == null) {
                        adapter = adapterFactory.adapterForCoin(coin, authData)
                        adapter?.start()
                    }
                    adapter
                }

                adaptersUpdatedSignal.onNext(Unit)

                oldAdapters.forEach { oldAdapter ->
                    if (adapters.none { it.coin.code == oldAdapter.coin.code }) {
                        oldAdapter.stop()
                        adapterFactory.unlinkAdapter(oldAdapter)
                    }
                }

                oldAdapters.clear()
            }
        }
    }

    override fun stopKits() {
        handler.post {
            adapters.forEach { it.stop() }
            adapters = listOf()
            adaptersUpdatedSignal.onNext(Unit)
        }
    }
}
