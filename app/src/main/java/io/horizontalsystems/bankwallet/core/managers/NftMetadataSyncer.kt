package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.adapters.nft.INftAdapter
import io.horizontalsystems.bankwallet.core.storage.NftStorage
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NftMetadataSyncer(
    private val nftAdapterManager: NftAdapterManager,
    private val nftMetadataManager: NftMetadataManager,
    private val nftStorage: NftStorage
) {
    private val syncThreshold: Long = 1 * 60 * 60 // 1 hour in seconds
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun start() {
        coroutineScope.launch {
            nftAdapterManager.adaptersUpdatedFlow.collect { adaptersMap ->
                launch {
                    sync(adaptersMap)
                }
            }
        }
    }

    fun refresh() {
        Log.e("AAA", "NftMetadataSyncer refresh()")

        coroutineScope.launch {
            sync(nftAdapterManager.adaptersUpdatedFlow.value, true)
        }
    }

    private suspend fun sync(adaptersMap: Map<NftKey, INftAdapter>, force: Boolean = false) =
        adaptersMap.forEach { (nftKey, adapter) ->
            sync(nftKey, adapter, force)
        }

    private suspend fun sync(nftKey: NftKey, adapter: INftAdapter, force: Boolean) {
        val currentTimestamp = System.currentTimeMillis() / 1000
        val lastSyncTimestamp = nftStorage.lastSyncTimestamp(nftKey)

        if (!force && lastSyncTimestamp != null && currentTimestamp - lastSyncTimestamp < syncThreshold) {
            return
        }

        try {
            val addressMetadata = nftMetadataManager.addressMetadata(nftKey.blockchainType, adapter.userAddress)
            handle(addressMetadata, nftKey, currentTimestamp)
        } catch (noProviderError: NftMetadataManager.ProviderError.NoProviderForBlockchainType) {
            //TODO
        } catch (error: Throwable) {
            Log.e("AAA", "NftMetadataSyncer: ${error.message}, blockchainType: ${nftKey.blockchainType}", error)
        }
    }

    private fun handle(addressMetadata: NftAddressMetadata, nftKey: NftKey, currentTimestamp: Long) {
        Log.e("AAA", "NftMetadataSyncer: handle addressMetadata")

        nftStorage.save(currentTimestamp, nftKey)
        nftMetadataManager.handle(addressMetadata, nftKey)
    }

}