package io.horizontalsystems.walletkit.modules.balance

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.balance.BalanceModule.BalanceWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class BalanceAdapterRepository(
    private val adapterManager: IAdapterManager,
    private val balanceCache: BalanceCache
) : Clearable {
    private var wallets = listOf<Wallet>()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val balanceStateUpdatedJobs = mutableListOf<Job>()
    private val balanceUpdatedJobs = mutableListOf<Job>()

    private val _readyFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val readyFlow: Flow<Unit> get() = _readyFlow

    // Unbounded, matching the Rx BUFFER strategy this replaced: each event names a
    // wallet whose row must re-read its data, so no event may be dropped.
    private val _updatesFlow = MutableSharedFlow<Wallet>(extraBufferCapacity = Int.MAX_VALUE)
    val updatesFlow: Flow<Wallet> get() = _updatesFlow

    init {
        coroutineScope.launch {
            adapterManager.adaptersReadyFlow.collectSafely {
                unsubscribeFromAdapterUpdates()
                _readyFlow.tryEmit(Unit)

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

    @Synchronized
    fun setWallet(wallets: List<Wallet>) {
        unsubscribeFromAdapterUpdates()
        this.wallets = wallets
        subscribeForAdapterUpdates()
    }

    @Synchronized
    private fun unsubscribeFromAdapterUpdates() {
        balanceStateUpdatedJobs.forEach { it.cancel() }
        balanceStateUpdatedJobs.clear()
        balanceUpdatedJobs.forEach { it.cancel() }
        balanceUpdatedJobs.clear()
    }

    @Synchronized
    private fun subscribeForAdapterUpdates() {
        wallets.forEach { wallet ->
            adapterManager.getBalanceAdapterForWallet(wallet)?.let { adapter ->
                balanceStateUpdatedJobs += coroutineScope.launch {
                    adapter.balanceStateUpdatedFlow.collectSafely {
                        _updatesFlow.tryEmit(wallet)
                    }
                }

                balanceUpdatedJobs += coroutineScope.launch {
                    adapter.balanceUpdatedFlow.collectSafely {
                        _updatesFlow.tryEmit(wallet)

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

    suspend fun warning(wallet: Wallet): BalanceWarning? {
        try {
            return ChainRegistry[wallet.token.blockchainType]?.balanceWarning(wallet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun refresh() {
        adapterManager.refresh()
    }

}