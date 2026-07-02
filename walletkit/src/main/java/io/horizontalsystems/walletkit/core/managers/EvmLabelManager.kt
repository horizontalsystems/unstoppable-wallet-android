package io.horizontalsystems.walletkit.core.managers

import android.util.Log
import io.horizontalsystems.walletkit.core.providers.EvmLabelProvider
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.core.storage.EvmAddressLabelDao
import io.horizontalsystems.walletkit.core.storage.EvmMethodLabelDao
import io.horizontalsystems.walletkit.core.storage.SyncerStateDao
import io.horizontalsystems.walletkit.core.toHexString
import io.horizontalsystems.walletkit.entities.EvmAddressLabel
import io.horizontalsystems.walletkit.entities.EvmMethodLabel
import io.horizontalsystems.walletkit.entities.SyncerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class EvmLabelManager(
    private val provider: EvmLabelProvider,
    private val addressLabelDao: EvmAddressLabelDao,
    private val methodLabelDao: EvmMethodLabelDao,
    private val syncerStateStorage: SyncerStateDao
) {
    private val keyMethodLabelsTimestamp = "evm-label-manager-method-labels-timestamp"
    private val keyAddressLabelsTimestamp = "evm-label-manager-address-labels-timestamp"

    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(singleDispatcher)

    fun sync() {
        coroutineScope.launch {
            try {
                val updatesStatus = provider.updatesStatus()
                syncMethodLabels(updatesStatus.evmMethodLabels)
                syncAddressLabels(updatesStatus.addressLabels)
            } catch (e: Exception) {
                Log.e("EvmLabelManager", "sync() error: ${e.message}", e)
            }
        }
    }

    fun methodLabel(input: ByteArray): String? {
        val methodId = input.take(4).toByteArray().toHexString()
        return methodLabelDao.get(methodId.lowercase())?.label
    }

    private fun addressLabel(address: String): String? {
        return addressLabelDao.get(address.lowercase())?.label
    }

    fun mapped(address: String): String {
        return addressLabel(address) ?: address.shorten()
    }

    private suspend fun syncAddressLabels(timestamp: Long) {
        val lastSyncTimestamp = syncerStateStorage.get(keyAddressLabelsTimestamp)?.value?.toLongOrNull()
        if (lastSyncTimestamp == timestamp) return

        val addressLabels = provider.evmAddressLabels()
        addressLabelDao.update(addressLabels.map { EvmAddressLabel(it.address.lowercase(), it.label) })

        syncerStateStorage.insert(SyncerState(keyAddressLabelsTimestamp, timestamp.toString()))
    }

    private suspend fun syncMethodLabels(timestamp: Long) {
        val lastSyncTimestamp = syncerStateStorage.get(keyMethodLabelsTimestamp)?.value?.toLongOrNull()
        if (lastSyncTimestamp == timestamp) return

        val methodLabels = provider.evmMethodLabels()
        methodLabelDao.update(methodLabels.map { EvmMethodLabel(it.methodId.lowercase(), it.label) })

        syncerStateStorage.insert(SyncerState(keyMethodLabelsTimestamp, timestamp.toString()))
    }

}
