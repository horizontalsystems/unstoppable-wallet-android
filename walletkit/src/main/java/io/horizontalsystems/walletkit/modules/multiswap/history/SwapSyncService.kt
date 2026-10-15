package io.horizontalsystems.walletkit.modules.multiswap.history

import android.util.Log
import io.horizontalsystems.walletkit.BuildConfig
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.managers.APIClient
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.walletkit.entities.SimulateFailSwapMode
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.walletkit.modules.multiswap.providers.MultiSwapProviderRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapSyncService(
    private val swapRecordManager: SwapRecordManager,
    appConfigProvider: IAppConfigProvider,
) : Clearable {

    private val unstoppableAPI = APIClient.build(
        appConfigProvider.uswapApiBaseUrl,
        mapOf("x-api-key" to appConfigProvider.uswapApiKey)
    ).create(UnstoppableAPI::class.java)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        coroutineScope.launch {
            while (isActive) {
                syncPending()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Reports a broadcast inbound tx hash to the provider's backend, off the send path: funds have
     * moved, so the screen must not wait on it, and a failure only costs the server its head start
     * on tracking (it discovers the deposit on its own).
     */
    fun reportInboundTxHash(provider: IMultiSwapProvider, providerSwapId: String, txHash: String) {
        coroutineScope.launch {
            try {
                provider.reportInboundTxHash(providerSwapId, txHash)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e("SwapSyncService", "Failed to report inbound tx hash for $providerSwapId: ${e.message}", e)
            }
        }
    }

    private suspend fun syncPending() {
        val pending = swapRecordManager.getPending()
        for (record in pending) {
            syncRecord(record)
        }
    }

    private suspend fun syncRecord(record: SwapRecord) {
        // A tracking handle means the app advances this record through its own
        // tracking service — the server tracker must leave it alone.
        if (record.trackingHandle != null) return

        if (record.providerId == AllBridgeTracker.PROVIDER_ID) {
            syncAllBridgeRecord(record)
            return
        }
        try {
            val call = SwapTrackRequestBuilder.build(record)
            var request = call.request
            // Only the stateless readers take the debug flag; /v3/track accepts the uuid alone.
            if (BuildConfig.DEBUG &&
                App.localStorage.simulateFailSwap == SimulateFailSwapMode.Server &&
                call.endpoint != SwapTrackRequestBuilder.Endpoint.Recorded
            ) {
                request = request.copy(testActionRequired = true)
            }
            val response = when (call.endpoint) {
                SwapTrackRequestBuilder.Endpoint.Recorded -> unstoppableAPI.track(request)
                SwapTrackRequestBuilder.Endpoint.Evm -> unstoppableAPI.trackEvm(request)
                SwapTrackRequestBuilder.Endpoint.Thorchain -> unstoppableAPI.trackThorchain(request)
            }

            if (record.transactionHash == null && response.hash != null) {
                swapRecordManager.updateTransactionHash(record.id, response.hash)
            }

            val outboundHash = response.legs
                ?.firstOrNull { leg ->
                    leg.type == "native_send" &&
                            (leg.toAsset == response.toAsset || leg.toAddress == response.toAddress) &&
                            leg.hash != null
                }
                ?.hash

            if (outboundHash != null && outboundHash != record.outboundTransactionHash) {
                swapRecordManager.updateOutboundTransactionHash(record.id, outboundHash)
            }

            val mappedStatus = mapStatus(response) ?: return
            val currentStatus = runCatching { SwapStatus.valueOf(record.status) }.getOrNull()
            val newPauseReason = response.meta?.pauseReason?.takeIf { mappedStatus == SwapStatus.ActionRequired }
            val statusChanged = mappedStatus != currentStatus
            val pauseReasonChanged = newPauseReason != record.pauseReason

            if (!statusChanged && !pauseReasonChanged) return

            val newAmountOut = response.toAmount?.toBigDecimalOrNull()
            if (newAmountOut != null && newAmountOut > BigDecimal.ZERO) {
                swapRecordManager.updateStatusAndAmountOut(record.id, mappedStatus, response.toAmount, newPauseReason)
            } else {
                swapRecordManager.updateStatus(record.id, mappedStatus, newPauseReason)
            }
        } catch (e: IllegalArgumentException) {
            // Provider not supported for tracking — skip silently
            Log.e("SwapSyncService", "Provider not supported for tracking: ${record.providerId}", e)
        } catch (e: Throwable) {
            Log.e("SwapSyncService", "Failed to sync record ${record.id}: ${e.message}", e)
        }
    }

    private suspend fun syncAllBridgeRecord(record: SwapRecord) {
        try {
            val txId = record.transactionHash ?: return
            val chain = AllBridgeTracker.chainSymbol(record.tokenInBlockchainTypeUid) ?: return
            val response = AllBridgeTracker.fetchTransferStatus(chain, txId)

            val outboundTxId = response.receive?.txId
            if (outboundTxId != null && outboundTxId != record.outboundTransactionHash) {
                swapRecordManager.updateOutboundTransactionHash(record.id, outboundTxId)
            }

            val newStatus = mapAllBridgeStatus(response)
                .takeIf { it != SwapStatus.valueOf(record.status) }
                ?: return
            val newAmountOut = response.receive?.amountFormatted?.takeIf { it > BigDecimal.ZERO }
            if (newAmountOut != null) {
                swapRecordManager.updateStatusAndAmountOut(record.id, newStatus, newAmountOut.toPlainString(), null)
            } else {
                swapRecordManager.updateStatus(record.id, newStatus, null)
            }
        } catch (e: Throwable) {
            Log.e("SwapSyncService", "Failed to sync AllBridge record ${record.id}: ${e.message}")
        }
    }

    private fun mapAllBridgeStatus(response: AllBridgeTracker.TransferStatus): SwapStatus {
        if (response.isSuspended) return SwapStatus.Failed
        val receive = response.receive
        if (receive == null) {
            val send = response.send
            return if (send.confirmations >= send.confirmationsNeeded) SwapStatus.Swapping else SwapStatus.Depositing
        }
        return if (receive.blockTime != null) SwapStatus.Completed else SwapStatus.Sending
    }

    private fun mapStatus(response: UnstoppableAPI.Response.Track): SwapStatus? = when (response.status) {
        // `signed`: the inbound tx hash is on the record, the deposit is not yet observed.
        "not_started", "signed" -> SwapStatus.Depositing
        "pending", "swapping" -> {
            val activeLeg = response.legs?.firstOrNull { it.status != "completed" }
            when (activeLeg?.type) {
                "swap" -> {
                    SwapStatus.Swapping
                }

                "native_send" -> {
                    val isOutbound = activeLeg.toAsset == response.toAsset || activeLeg.toAddress == response.toAddress
                    if (isOutbound) SwapStatus.Sending else SwapStatus.Depositing
                }

                else -> SwapStatus.Swapping
            }
        }

        "completed" -> SwapStatus.Completed
        "refunded" -> SwapStatus.Refunded
        "failed" -> SwapStatus.Failed
        "expired" -> SwapStatus.Expired
        "action_required" -> SwapStatus.ActionRequired
        else -> null // "unknown" — leave status unchanged
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L
    }
}
