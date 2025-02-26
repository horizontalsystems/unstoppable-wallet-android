package io.horizontalsystems.solanakit.core

import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.noderpc.ApiSyncer
import io.horizontalsystems.solanakit.noderpc.IApiSyncerListener
import io.horizontalsystems.solanakit.noderpc.SyncerState
import io.horizontalsystems.solanakit.transactions.ITransactionListener
import io.horizontalsystems.solanakit.transactions.TransactionManager
import io.horizontalsystems.solanakit.transactions.TransactionSyncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface ISyncListener {
    fun onUpdateBalanceSyncState(syncState: SolanaKit.SyncState)
    fun onUpdateTokenSyncState(syncState: SolanaKit.SyncState)
    fun onUpdateTransactionSyncState(syncState: SolanaKit.SyncState)
    fun onUpdateLastBlockHeight(lastBlockHeight: Long)
    fun onUpdateBalance(balance: Long)
}

class SyncManager(
    private val apiSyncer: ApiSyncer,
    private val balanceSyncer: BalanceManager,
    private val tokenAccountSyncer: TokenAccountManager,
    private val transactionSyncer: TransactionSyncer,
    private val transactionManager: TransactionManager
) : IApiSyncerListener, IBalanceListener, ITokenAccountListener, ITransactionListener {

    private var scope: CoroutineScope? = null

    var listener: ISyncListener? = null

    val balanceSyncState: SolanaKit.SyncState
        get() = balanceSyncer.syncState

    val tokenBalanceSyncState: SolanaKit.SyncState
        get() = tokenAccountSyncer.syncState

    val transactionsSyncState: SolanaKit.SyncState
        get() = transactionSyncer.syncState

    private var started = false

    init {
        balanceSyncer.listener = this
        apiSyncer.listener = this
        tokenAccountSyncer.listener = this
        transactionSyncer.listener = this
    }

    suspend fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        this.scope = scope

        apiSyncer.start(scope)

        scope.launch {
            transactionManager.transactionsFlow
                .collect { balanceSyncer.sync() }
        }
    }

    suspend fun refresh(scope: CoroutineScope) {
        if (apiSyncer.state !is SyncerState.Ready) {
            apiSyncer.stop()
            apiSyncer.start(scope)
        } else {
            balanceSyncer.sync()
            tokenAccountSyncer.sync()
            transactionSyncer.sync()
        }
    }

    fun stop() {
        started = false
        scope = null

        apiSyncer.stop()
        balanceSyncer.stop()
        tokenAccountSyncer.stop()
    }

    override fun didUpdateApiState(state: SyncerState) {
        when (state) {
            SyncerState.Ready -> Unit

            is SyncerState.NotReady -> {
                scope?.launch {
                    balanceSyncer.stop(state.error)
                    tokenAccountSyncer.stop(state.error)
                }
            }
        }
    }

    override fun onUpdateTokenSyncState(value: SolanaKit.SyncState) {
        scope?.launch {
            listener?.onUpdateTokenSyncState(value)
        }
    }

    override fun onUpdateTransactionSyncState(syncState: SolanaKit.SyncState) {
        scope?.launch {
            listener?.onUpdateTransactionSyncState(syncState)
        }
    }

    override fun onUpdateBalanceSyncState(value: SolanaKit.SyncState) {
        scope?.launch {
            listener?.onUpdateBalanceSyncState(value)
        }
    }

    override fun didUpdateLastBlockHeight(lastBlockHeight: Long) {
        scope?.launch {
            listener?.onUpdateLastBlockHeight(lastBlockHeight)

            balanceSyncer.sync()
            tokenAccountSyncer.sync()
            transactionSyncer.sync()
        }
    }

    override fun onUpdateBalance(balance: Long) {
        scope?.launch {
            listener?.onUpdateBalance(balance)
        }
    }

}
