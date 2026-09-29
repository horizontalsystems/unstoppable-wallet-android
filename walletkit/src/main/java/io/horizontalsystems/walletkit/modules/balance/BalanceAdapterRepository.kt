package io.horizontalsystems.walletkit.modules.balance

import android.os.SystemClock
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.IAdapterManager
import io.horizontalsystems.walletkit.core.ITorManager
import io.horizontalsystems.walletkit.core.TorUnsupportedException
import io.horizontalsystems.walletkit.core.managers.TorManager
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.balance.BalanceModule.BalanceWarning
import io.horizontalsystems.walletkit.modules.settings.privacy.tor.TorStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class BalanceAdapterRepository(
    private val adapterManager: IAdapterManager,
    private val balanceCache: BalanceCache,
    private val torManager: ITorManager,
) : Clearable {
    private var wallets = listOf<Wallet>()
    @Volatile
    private var torConnectedAt = 0L

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
        coroutineScope.launch {
            torManager.torStatusFlow.collectSafely { status ->
                if (status == TorStatus.Connected) {
                    torConnectedAt = SystemClock.elapsedRealtime()
                }
                wallets.forEach { _updatesFlow.tryEmit(it) }
                if (status == TorStatus.Connected) {
                    delay(TOR_ERROR_GRACE_MILLIS)
                    wallets.forEach { _updatesFlow.tryEmit(it) }
                }
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
        val state = adapterManager.getBalanceAdapterForWallet(wallet)?.balanceState
            ?: AdapterState.Syncing()

        // Nothing reaches the network while Tor starts or reconnects, so errors from kits that
        // tried meanwhile are not real. The kits are refreshed once Tor connects and those still
        // failing once more after TorManager.WARM_UP_MILLIS, so errors stay hidden until then.
        if (state is AdapterState.NotSynced && state.error !is TorUnsupportedException && torManager.isTorEnabled) {
            val torStatus = torManager.torStatusFlow.value
            val justConnected = torStatus == TorStatus.Connected &&
                SystemClock.elapsedRealtime() - torConnectedAt < TOR_ERROR_GRACE_MILLIS
            if (torStatus == TorStatus.Connecting || justConnected) {
                return AdapterState.Connecting
            }
        }
        return state
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
        // A refresh on the circuit that just failed would fail the same way on a refusing exit
        if (torManager.isTorEnabled) {
            torManager.newCircuits()
        }
        adapterManager.refresh()
    }

    companion object {
        private const val TOR_ERROR_GRACE_MILLIS = TorManager.WARM_UP_MILLIS + 10_000L
    }
}
