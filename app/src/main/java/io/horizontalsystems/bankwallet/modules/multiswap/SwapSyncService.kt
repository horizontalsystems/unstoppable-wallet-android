package io.horizontalsystems.bankwallet.modules.multiswap

import android.util.Log
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.OneInchProvider
import io.horizontalsystems.bankwallet.modules.multiswap.providers.UnstoppableAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SwapSyncService(
    private val swapRecordManager: SwapRecordManager,
    appConfigProvider: AppConfigProvider,
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

    private suspend fun syncPending() {
        val pending = swapRecordManager.getPending()
        for (record in pending) {
            syncRecord(record)
        }
    }

    private suspend fun syncRecord(record: SwapRecord) {
        try {
            val request = SwapTrackRequestBuilder.build(record)
            Log.e("eee", "request: $request")
            val response = if (record.providerId == OneInchProvider.id) {
                unstoppableAPI.trackEvm(request)
            } else {
                unstoppableAPI.track(request)
            }
            Log.e("eee", "response: $response")
            val newStatus = mapStatus(response)
                ?.takeIf { it != SwapStatus.valueOf(record.status) }
                ?: return
            val newAmountOut = response.toAmount?.takeIf { it.isNotEmpty() }
            if (newAmountOut != null) {
                swapRecordManager.updateStatusAndAmountOut(record.id, newStatus, newAmountOut)
            } else {
                swapRecordManager.updateStatus(record.id, newStatus)
            }
        } catch (e: IllegalArgumentException) {
            // Provider not supported for tracking — skip silently
            Log.e("SwapSyncService", "Provider not supported for tracking: ${record.providerId}", e)
        } catch (e: Throwable) {
            Log.e("SwapSyncService", "Failed to sync record ${record.id}: ${e.message}")
        }
    }

    private fun mapStatus(response: UnstoppableAPI.Response.Track): SwapStatus? = when (response.status) {
        "not_started" -> SwapStatus.Depositing
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
        else -> null // "unknown" — leave status unchanged
    }

    override fun clear() {
        coroutineScope.cancel()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L
    }
}
