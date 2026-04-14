package com.quantum.wallet.bankwallet.modules.multiswap.history

import android.util.Log
import com.quantum.wallet.bankwallet.core.Clearable
import com.quantum.wallet.bankwallet.core.managers.APIClient
import com.quantum.wallet.bankwallet.core.providers.AppConfigProvider
import com.quantum.wallet.bankwallet.entities.SwapRecord
import com.quantum.wallet.bankwallet.modules.multiswap.providers.AllBridgeAPI
import com.quantum.wallet.bankwallet.modules.multiswap.providers.AllBridgeProvider
import com.quantum.wallet.bankwallet.modules.multiswap.providers.OneInchProvider
import com.quantum.wallet.bankwallet.modules.multiswap.providers.UnstoppableAPI
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
        if (record.providerId == AllBridgeProvider.id) {
            syncAllBridgeRecord(record)
            return
        }
        try {
            val request = SwapTrackRequestBuilder.build(record)
            val response = if (record.providerId == OneInchProvider.id) {
                unstoppableAPI.trackEvm(request)
            } else {
                unstoppableAPI.track(request)
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

            val newStatus = mapStatus(response)
                ?.takeIf { it != SwapStatus.valueOf(record.status) }
                ?: return
            val newAmountOut = response.toAmount?.toBigDecimalOrNull()
            if (newAmountOut != null && newAmountOut > BigDecimal.ZERO) {
                swapRecordManager.updateStatusAndAmountOut(record.id, newStatus, response.toAmount)
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

    private suspend fun syncAllBridgeRecord(record: SwapRecord) {
        try {
            val txId = record.transactionHash ?: return
            val chain = AllBridgeProvider.chainSymbol(record.tokenInBlockchainTypeUid) ?: return
            val response = AllBridgeProvider.fetchTransferStatus(chain, txId)

            val outboundTxId = response.receive?.txId
            if (outboundTxId != null && outboundTxId != record.outboundTransactionHash) {
                swapRecordManager.updateOutboundTransactionHash(record.id, outboundTxId)
            }

            val newStatus = mapAllBridgeStatus(response)
                .takeIf { it != SwapStatus.valueOf(record.status) }
                ?: return
            val newAmountOut = response.receive?.amountFormatted?.takeIf { it > BigDecimal.ZERO }
            if (newAmountOut != null) {
                swapRecordManager.updateStatusAndAmountOut(record.id, newStatus, newAmountOut.toPlainString())
            } else {
                swapRecordManager.updateStatus(record.id, newStatus)
            }
        } catch (e: Throwable) {
            Log.e("SwapSyncService", "Failed to sync AllBridge record ${record.id}: ${e.message}")
        }
    }

    private fun mapAllBridgeStatus(response: AllBridgeAPI.Response.TransferStatus): SwapStatus {
        if (response.isSuspended) return SwapStatus.Failed
        val receive = response.receive
        if (receive == null) {
            val send = response.send
            return if (send.confirmations >= send.confirmationsNeeded) SwapStatus.Swapping else SwapStatus.Depositing
        }
        return if (receive.blockTime != null) SwapStatus.Completed else SwapStatus.Sending
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
