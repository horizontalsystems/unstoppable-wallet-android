package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.adapters.BaseTronAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule.BalanceWarning
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal

class BalanceAdapterRepository(
    private val adapterManager: IAdapterManager,
    private val balanceCache: BalanceCache
) : Clearable {
    private var wallets = listOf<Wallet>()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var balanceStateUpdatedJob: Job? = null
    private var balanceUpdatedJob: Job? = null

    private val readySubject = PublishSubject.create<Unit>()
    val readyObservable: Observable<Unit> get() = readySubject

    private val updatesSubject = PublishSubject.create<Wallet>()
    val updatesObservable: Observable<Wallet> get() = updatesSubject

    init {
        coroutineScope.launch {
            adapterManager.adaptersReadyObservable.asFlow().collect {
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
    }

    override fun clear() {
        unsubscribeFromAdapterUpdates()
        coroutineScope.cancel()
    }

    fun setWallet(wallets: List<Wallet>) {
        unsubscribeFromAdapterUpdates()
        this.wallets = wallets
        subscribeForAdapterUpdates()
    }

    private fun unsubscribeFromAdapterUpdates() {
        balanceStateUpdatedJob?.cancel()
        balanceUpdatedJob?.cancel()
    }

    private fun subscribeForAdapterUpdates() {
        wallets.forEach { wallet ->
            adapterManager.getBalanceAdapterForWallet(wallet)?.let { adapter ->
                balanceStateUpdatedJob = coroutineScope.launch {
                    adapter.balanceStateUpdatedFlowable.asFlow().collect {
                        updatesSubject.onNext(wallet)
                    }
                }

                balanceUpdatedJob = coroutineScope.launch {
                    adapter.balanceUpdatedFlowable.asFlow().collect {
                        updatesSubject.onNext(wallet)

                        adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.let {
                            balanceCache.setCache(wallet, it)
                        }
                    }
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

    suspend fun warning(wallet: Wallet): BalanceWarning? {
        if (wallet.token.blockchainType is BlockchainType.Tron) {
            (adapterManager.getAdapterForWallet(wallet) as? BaseTronAdapter)?.let { adapter ->
                if (!adapter.isAddressActive(adapter.receiveAddress))
                    return BalanceWarning.TronInactiveAccountWarning
            }
        }
        return null
    }

    fun refresh() {
        adapterManager.refresh()
    }

}