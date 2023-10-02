package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

class BalanceAdapterRepository(
    private val adapterManager: IAdapterManager,
    private val balanceCache: BalanceCache
) : Clearable {
    private var wallets = listOf<Wallet>()

    private val updatesDisposables = CompositeDisposable()
    private var adapterReadyDisposable: Disposable? = null

    private val readySubject = PublishSubject.create<Unit>()
    val readyObservable: Observable<Unit> get() = readySubject

    private val updatesSubject = PublishSubject.create<Wallet>()
    val updatesObservable: Observable<Wallet> get() = updatesSubject

    init {
        subscribeForAdapterReadyUpdate()
    }

    override fun clear() {
        unsubscribeFromAdapterUpdates()
        unsubscribeFromAdapterReadyUpdate()
    }

    fun setWallet(wallets: List<Wallet>) {
        unsubscribeFromAdapterUpdates()
        this.wallets = wallets
        subscribeForAdapterUpdates()
    }

    private fun unsubscribeFromAdapterReadyUpdate() {
        adapterReadyDisposable?.dispose()
    }

    private fun subscribeForAdapterReadyUpdate() {
        adapterReadyDisposable = adapterManager.adaptersReadyObservable
            .subscribeIO {
                unsubscribeFromAdapterUpdates()
                readySubject.onNext(Unit)

                balanceCache.setCache(
                    wallets.mapNotNull { wallet ->
                        adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.let {
                            wallet to it
                        }
                    }.toMap()
                )

                subscribeForAdapterUpdates()
            }
    }
    private fun unsubscribeFromAdapterUpdates() {
        updatesDisposables.clear()
    }

    private fun subscribeForAdapterUpdates() {
        wallets.forEach { wallet ->
            adapterManager.getBalanceAdapterForWallet(wallet)?.let { adapter ->
                adapter.balanceStateUpdatedFlowable
                    .subscribeIO {
                        updatesSubject.onNext(wallet)
                    }
                    .let {
                        updatesDisposables.add(it)
                    }

                adapter.balanceUpdatedFlowable
                    .subscribeIO {
                        updatesSubject.onNext(wallet)

                        adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.let {
                            balanceCache.setCache(wallet, it)
                        }
                    }
                    .let {
                        updatesDisposables.add(it)
                    }
            }
        }
    }

    fun state(wallet: Wallet): AdapterState {
        return adapterManager.getBalanceAdapterForWallet(wallet)?.balanceState
            ?: AdapterState.Syncing()
    }

    fun balanceData(wallet: Wallet): BalanceData {
        return adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData
            ?: balanceCache.getCache(wallet)
            ?: BalanceData(BigDecimal.ZERO)
    }

    fun sendAllowed(wallet: Wallet): Boolean {
        return adapterManager.getBalanceAdapterForWallet(wallet)?.sendAllowed() ?: false
    }

    fun refresh() {
        adapterManager.refresh()
    }

}