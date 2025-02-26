package io.horizontalsystems.solanakit.core

import com.solana.api.Api
import com.solana.api.getBalance
import com.solana.core.PublicKey
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.database.main.MainStorage

interface IBalanceListener {
    fun onUpdateBalanceSyncState(value: SolanaKit.SyncState)
    fun onUpdateBalance(balance: Long)
}

class BalanceManager(
    private val publicKey: PublicKey,
    private val rpcClient: Api,
    private val storage: MainStorage
) {
    var syncState: SolanaKit.SyncState =
        SolanaKit.SyncState.NotSynced(SolanaKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.onUpdateBalanceSyncState(value)
            }
        }

    var listener: IBalanceListener? = null

    var balance: Long? = storage.getBalance()
        private set


    fun stop(error: Throwable? = null) {
        syncState = SolanaKit.SyncState.NotSynced(error ?: SolanaKit.SyncError.NotStarted())
    }

    suspend fun sync() {
        if (syncState is SolanaKit.SyncState.Syncing) return

        syncState = SolanaKit.SyncState.Syncing()

        try {
            val balance = rpcClient.getBalance(publicKey).getOrThrow()
            handleBalance(balance)
        } catch (error: Throwable) {
            syncState = SolanaKit.SyncState.NotSynced(error)
        }
    }

    private fun handleBalance(balance: Long) {
        if (this.balance != balance) {
            this.balance = balance
            storage.saveBalance(balance)
            listener?.onUpdateBalance(balance)
        }

        syncState = SolanaKit.SyncState.Synced()
    }
}