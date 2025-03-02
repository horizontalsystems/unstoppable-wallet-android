package io.horizontalsystems.solanakit.noderpc

import com.solana.api.Api
import com.solana.rxsolana.api.getBlockHeight
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.database.main.MainStorage
import io.horizontalsystems.solanakit.network.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface IApiSyncerListener {
    fun didUpdateApiState(state: SyncerState)
    fun didUpdateLastBlockHeight(lastBlockHeight: Long)
}

sealed class SyncerState {
    object Ready : SyncerState()
    class NotReady(val error: Throwable) : SyncerState()
}

class ApiSyncer(
    private val api: Api,
    private val syncInterval: Long,
    private val connectionManager: ConnectionManager,
    private val storage: MainStorage
) {

    private var scope: CoroutineScope? = null
    private var isStarted = false
    private var timerJob: Job? = null

    init {
        connectionManager.listener = object : ConnectionManager.Listener {
            override fun onConnectionChange() {
                handleConnectionChange()
            }
        }
    }

    var state: SyncerState = SyncerState.NotReady(SolanaKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.didUpdateApiState(value)
            }
        }

    var listener: IApiSyncerListener? = null
    val source = "API ${api.router.endpoint.url.host}"

    var lastBlockHeight: Long? = storage.getLastBlockHeight()
        private set

    fun start(scope: CoroutineScope) {
        isStarted = true
        this.scope = scope

        handleConnectionChange()
    }

    fun stop() {
        isStarted = false

        connectionManager.stop()
        state = SyncerState.NotReady(SolanaKit.SyncError.NotStarted())
        scope = null
        stopTimer()
    }

    private suspend fun sync() = withContext(Dispatchers.IO) {
        try {
            val blockHeight = api.getBlockHeight().await()
            handleBlockHeight(blockHeight)
        } catch (error: Throwable) {
            state = SyncerState.NotReady(error)
        }
    }

    private fun handleBlockHeight(blockHeight: Long) {
        if (this.lastBlockHeight != blockHeight) {
            this.lastBlockHeight = blockHeight
            storage.saveLastBlockHeight(blockHeight)
        }

        listener?.didUpdateLastBlockHeight(blockHeight)
    }

    private fun handleConnectionChange() {
        if (!isStarted) return

        if (connectionManager.isConnected) {
            state = SyncerState.Ready
            startTimer()
        } else {
            state = SyncerState.NotReady(SolanaKit.SyncError.NoNetworkConnection())
            stopTimer()
        }
    }

    private fun startTimer() {
        timerJob = scope?.launch {
            flow {
                while (isActive) {
                    emit(Unit)
                    delay(syncInterval.toDuration(DurationUnit.SECONDS))
                }
            }.collect {
                sync()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

}
